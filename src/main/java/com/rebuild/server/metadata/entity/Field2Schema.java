/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.metadata.entity;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.metadata.impl.FieldImpl;
import cn.devezhao.persist4j.util.StringHelper;
import cn.devezhao.persist4j.util.support.Table;
import com.alibaba.fastjson.JSON;
import com.hankcs.hanlp.HanLP;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.helper.BlackList;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * 创建字段
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class Field2Schema {

	private static final Log LOG = LogFactory.getLog(Field2Schema.class);

	// 小数位真实长度
	private static final int DECIMAL_SCALE = 8;
	
	final protected ID user;
	final protected Set<ID> tempMetaId = new HashSet<>();
	
	/**
	 * @param user
	 */
	public Field2Schema(ID user) {
		this.user = user;
		Assert.isTrue(UserHelper.isSuperAdmin(user), "仅超级管理员可添加/删除元数据");
	}
	
	/**
	 * @param entity
	 * @param fieldLabel
	 * @param type
	 * @param comments
	 * @param refEntity
	 * @param extConfig
	 * @return
	 */
	public String createField(Entity entity, String fieldLabel, DisplayType type, String comments, String refEntity, JSON extConfig) {
		long count;
		if ((count = checkRecordCount(entity)) > 100000) {
			throw new ModifiyMetadataException("本实体记录过大，增加字段可能导致表损坏 (记录数: " + count + ")");
		}
		
		String fieldName = toPinyinName(fieldLabel);
		while (true) {
			if (entity.containsField(fieldName) || MetadataHelper.isCommonsField(fieldName)) {
				fieldName += (10 + RandomUtils.nextInt(89));
			} else {
				break;
			}
		}
		
		Field field = createUnsafeField(
				entity, fieldName, fieldLabel, type, true, true, true, true, comments, refEntity, null, extConfig, null);
		
		boolean schemaReady = schema2Database(entity, new Field[] { field });
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[0]));
			throw new ModifiyMetadataException("无法创建字段到数据库");
		}
		
		Application.getMetadataFactory().refresh(false);
		return fieldName;
	}
	
	/**
	 * @param field
	 * @return
	 */
    public boolean dropField(Field field) {
		return dropField(field, false);
	}
	
	/**
	 * @param field
	 * @param force
	 * @return
	 */
	public boolean dropField(Field field, boolean force) {
		EasyMeta easyMeta = EasyMeta.valueOf(field);
		ID metaRecordId = easyMeta.getMetaId();
		if (easyMeta.isBuiltin() || metaRecordId == null) {
			throw new ModifiyMetadataException("系统内建字段不允许删除");
		}
		
		Entity entity = field.getOwnEntity();
		if (entity.getNameField().equals(field)) {
			throw new ModifiyMetadataException("名称字段不允许被删除");
		}
		
		if (!force) {
			long count;
			if ((count = checkRecordCount(entity)) > 100000) {
				throw new ModifiyMetadataException("本实体记录过大，删除字段可能导致表损坏 (" + entity.getName() + "=" + count + ")");
			}
		}
		
		String ddl = String.format("alter table `%s` drop column `%s`", entity.getPhysicalName(), field.getPhysicalName());
		try {
			Application.getSQLExecutor().execute(ddl, 10 * 60);
		} catch (Throwable ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			return false;
		}
		
		Application.getBean(MetaFieldService.class).delete(metaRecordId);
		Application.getMetadataFactory().refresh(false);
		return true;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	protected long checkRecordCount(Entity entity) {
		String sql = String.format("select count(%s) from %s", entity.getPrimaryField().getName(), entity.getName());
		Object[] count = Application.createQueryNoFilter(sql).unique();
		return ObjectUtils.toLong(count[0]);
	}
	
	/**
	 * @param entity
	 * @param fields
	 * @return
	 */
	protected boolean schema2Database(Entity entity, Field[] fields) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		StringBuilder ddl = new StringBuilder("alter table `" + entity.getPhysicalName() + "`");
		for (Field field : fields) {
			ddl.append("\n  add column ");
			table.generateFieldDDL(field, ddl);
			ddl.append(",");
		}
		ddl.deleteCharAt(ddl.length() - 1);
		
		try {
			Application.getSQLExecutor().executeBatch(new String[] { ddl.toString() }, 10 * 60);
		} catch (Throwable ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			return false;
		}
		return true;
	}
	
	/**
	 * 内部用。注意此方法不会添加列到数据库
	 * 
	 * @param entity
	 * @param fieldName
	 * @param fieldLabel
	 * @param dt
	 * @param nullable
	 * @param creatable
	 * @param updatable
	 * @param repeatable
	 * @param comments
	 * @param refEntity
	 * @param cascade
	 * @param extConfig
	 * @param defaultValue
	 * @return
	 * @see #createField(Entity, String, DisplayType, String, String, JSON)
	 */
	public Field createUnsafeField(Entity entity, String fieldName, String fieldLabel, DisplayType dt,
			boolean nullable, boolean creatable, boolean updatable, boolean repeatable, String comments, String refEntity, CascadeModel cascade,
			JSON extConfig, Object defaultValue) {
		if (dt == DisplayType.SERIES) {
			nullable = false;
			creatable = false;
			updatable = false;
			repeatable = false;
		} else if (EntityHelper.AutoId.equalsIgnoreCase(fieldName)) {
			repeatable = false;
		}

		Record recordOfField = EntityHelper.forNew(EntityHelper.MetaField, user);
		recordOfField.setString("belongEntity", entity.getName());
		recordOfField.setString("fieldName", fieldName);
//		String physicalName = fieldName.toUpperCase();
		String physicalName = StringHelper.hyphenate(fieldName).toUpperCase();
		recordOfField.setString("physicalName", physicalName);
		recordOfField.setString("fieldLabel", fieldLabel);
		recordOfField.setString("displayType", dt.name());
		recordOfField.setBoolean("nullable", nullable);
		recordOfField.setBoolean("creatable", creatable);
		recordOfField.setBoolean("updatable", updatable);
		recordOfField.setBoolean("repeatable", repeatable);
		if (StringUtils.isNotBlank(comments)) {
			recordOfField.setString("comments", comments);
		}
		if (defaultValue != null) {
			recordOfField.setString("defaultValue", defaultValue.toString());
		}
		
		if (dt == DisplayType.PICKLIST) {
			refEntity = "PickList";
		} else if (dt == DisplayType.CLASSIFICATION) {
			refEntity = "ClassificationData";
		}

        if (extConfig != null) {
            recordOfField.setString("extConfig", extConfig.toJSONString());
        }
		
		if (StringUtils.isNotBlank(refEntity)) {
			if (!MetadataHelper.containsEntity(refEntity)) {
				throw new ModifiyMetadataException("无效引用实体: " + refEntity);
			}
			recordOfField.setString("refEntity", refEntity);
			if (cascade != null) {
				String cascadeAlias = cascade == CascadeModel.RemoveLinks ? "remove-links" : cascade.name().toLowerCase();
				recordOfField.setString("cascade", cascadeAlias);
			} else {
				recordOfField.setString("cascade", CascadeModel.Ignore.name().toLowerCase());
			}
		}
		
		int maxLength = dt.getMaxLength();
		if (EntityHelper.QuickCode.equalsIgnoreCase(fieldName)) {
			maxLength = 70;
		}
		recordOfField.setInt("maxLength", maxLength);
		
		if (dt == DisplayType.REFERENCE && StringUtils.isBlank(refEntity)) {
			throw new ModifiyMetadataException("引用字段必须指定引用实体");
		}
		
		recordOfField = Application.getCommonService().create(recordOfField);
		tempMetaId.add(recordOfField.getPrimary());
		
		// 此处会改变一些属性，因为并不想他们同步到数据库 SCHEMA
		
		boolean autoValue = EntityHelper.AutoId.equalsIgnoreCase(fieldName);
		if (EntityHelper.ApprovalState.equalsIgnoreCase(fieldName)) {
			defaultValue = ApprovalState.DRAFT.getState();
		}

		// 系统级字段非空
        if (MetadataHelper.isCommonsField(fieldName)
                && !(MetadataHelper.isApprovalField(fieldName) || fieldName.equalsIgnoreCase(EntityHelper.QuickCode))) {
            nullable = false;
        } else {
            nullable = true;
        }

		Field unsafeField = new FieldImpl(
				fieldName, physicalName, fieldLabel, entity, dt.getFieldType(), CascadeModel.Ignore, maxLength,
				nullable, creatable, updatable, repeatable, DECIMAL_SCALE, defaultValue, autoValue);
		if (entity instanceof UnsafeEntity) {
			((UnsafeEntity) entity).addField(unsafeField);
		}
		return unsafeField;
	}
	
	/**
	 * 中文 -> 拼音（仅保留字母数字）
	 * 
	 * @param text
	 * @return
	 */
	protected String toPinyinName(final String text) {
		String identifier = text;
		if (text.length() < 4) {
			identifier = "rb" + text + RandomUtils.nextInt(10);
		}
		
		// 全英文直接返回
		if (identifier.matches("[a-zA-Z0-9]+")) {
			if (!CharSet.ASCII_ALPHA.contains(identifier.charAt(0))
					|| BlackList.isBlack(identifier) || BlackList.isSQLKeyword(identifier)) {
				identifier = "rb" + identifier;
			}
			return identifier;
		}
		
		identifier = HanLP.convertToPinyinString(identifier, "", false);
		identifier = identifier.replaceAll("[^a-zA-Z0-9]", "");
		if (StringUtils.isBlank(identifier)) {
			throw new ModifiyMetadataException("无效名称 : " + text);
		}
		
		char start = identifier.charAt(0);
		if (!CharSet.ASCII_ALPHA.contains(start)) {
			identifier = "rb" + identifier;
		}
		
		identifier = identifier.toLowerCase();
		if (identifier.length() > 42) {
			identifier = identifier.substring(0, 42);
		}
		
		if (!StringHelper.isIdentifier(identifier)) {
			throw new ModifiyMetadataException("无效名称 : " + text);
		}
		return identifier;
	}
}
