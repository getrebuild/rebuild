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

package com.rebuild.server.metadata.entityhub;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.hankcs.hanlp.HanLP;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;

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

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class Field2Schema {

	private static final Log LOG = LogFactory.getLog(Field2Schema.class);
	
	final protected ID user;
	final protected Set<ID> tempMetaId = new HashSet<>();
	
	/**
	 * @param user
	 */
	public Field2Schema(ID user) {
		this.user = user;
		Assert.isTrue(UserHelper.isSuperAdmin(user), "仅超级管理员可新建/删除元数据");
	}
	
	/**
	 * @param entity
	 * @param fieldLabel
	 * @param type
	 * @param comments
	 * @return
	 */
	public String create(Entity entity, String fieldLabel, DisplayType type, String comments) {
		return create(entity, fieldLabel, type, comments, null, null);
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
	public String create(Entity entity, String fieldLabel, DisplayType type, String comments, String refEntity, JSON extConfig) {
		long count = 0;
		if ((count = checkRecordCount(entity)) > 50000) {
			throw new ModifiyMetadataException("本实体记录过大，增加字段可能导致表损坏 (" + entity.getName() + "=" + count + ")");
		}
		
		String fieldName = toPinyinName(fieldLabel);
		while (true) {
			if (entity.containsField(fieldName)) {
				fieldName += (10 + RandomUtils.nextInt(89));
			} else {
				break;
			}
		}
		
		Field field = createUnsafeField(
				entity, fieldName, fieldLabel, type, true, true, true, comments, refEntity, null, true, extConfig, null);
		
		boolean schemaReady = schema2Database(entity, field);
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[tempMetaId.size()]));
			return null;
		}
		
		Application.getMetadataFactory().refresh(false);
		return fieldName;
	}
	
	/**
	 * @param field
	 * @return
	 */
	public boolean drop(Field field) {
		return drop(field, false);
	}
	
	/**
	 * @param field
	 * @param force
	 * @return
	 */
	public boolean drop(Field field, boolean force) {
		if (!user.equals(UserService.ADMIN_USER)) {
			throw new ModifiyMetadataException("仅超级管理员可删除字段");
		}
		
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
			long count = 0;
			if ((count = checkRecordCount(entity)) > 50000) {
				throw new ModifiyMetadataException("本实体记录过大，删除字段可能导致表损坏 (" + entity.getName() + "=" + count + ")");
			}
		}
		
		String ddl = String.format("alter table `%s` drop column `%s`", entity.getPhysicalName(), field.getPhysicalName());
		try {
			Application.getSQLExecutor().execute(ddl);
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
	
	private boolean schema2Database(Entity entity, Field field) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		StringBuilder ddl = new StringBuilder("alter table `" + entity.getPhysicalName() + "`\n  add column ");
		table.generateFieldDDL(field, ddl);
		try {
			Application.getSQLExecutor().executeBatch(new String[] { ddl.toString() });
		} catch (Throwable ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param entity
	 * @param fieldName
	 * @param fieldLabel
	 * @param displayType
	 * @param nullable
	 * @param creatable
	 * @param updatable
	 * @param comments
	 * @param refEntity
	 * @param cascade
	 * @param nullableInDb 在数据库中是否可为空，一般系统级字段不能为空
	 * @param extConfig
	 * @param defaultValue
	 * @return
	 */
	public Field createUnsafeField(Entity entity, String fieldName, String fieldLabel, DisplayType displayType,
			boolean nullable, boolean creatable, boolean updatable, String comments, String refEntity, CascadeModel cascade,
			boolean nullableInDb, JSON extConfig, String defaultValue) {
		if (displayType == DisplayType.SERIES) {
			nullable = false;
			creatable = false;
			updatable = false;
			nullableInDb = false;
		}
		
		Record recordOfField = EntityHelper.forNew(EntityHelper.MetaField, user);
		recordOfField.setString("belongEntity", entity.getName());
		recordOfField.setString("fieldName", fieldName);
		String physicalName = fieldName.toUpperCase();
		recordOfField.setString("physicalName", physicalName);
		recordOfField.setString("fieldLabel", fieldLabel);
		recordOfField.setString("displayType", displayType.name());
		recordOfField.setBoolean("nullable", nullable);
		recordOfField.setBoolean("creatable", creatable);
		recordOfField.setBoolean("updatable", updatable);
		if (StringUtils.isNotBlank(comments)) {
			recordOfField.setString("comments", comments);
		}
		if (StringUtils.isNotBlank(defaultValue)) {
			recordOfField.setString("defaultValue", defaultValue);
		}
		
		if (displayType == DisplayType.PICKLIST) {
			refEntity = "PickList";
		} else if (displayType == DisplayType.CLASSIFICATION) {
			refEntity = "ClassificationData";
			if (extConfig != null) {
				recordOfField.setString("extConfig", extConfig.toJSONString());
			}
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
		
		int maxLength = displayType.getMaxLength();
		if (EntityHelper.QuickCode.equals(fieldName)) {
			maxLength = 70;
		}
		recordOfField.setInt("maxLength", maxLength);
		
		if (displayType == DisplayType.REFERENCE && StringUtils.isBlank(refEntity)) {
			throw new ModifiyMetadataException("引用字段必须指定引用实体");
		}
		
		recordOfField = Application.getCommonService().create(recordOfField);
		tempMetaId.add(recordOfField.getPrimary());
		
		boolean autoValue = EntityHelper.AutoId.equals(fieldName);
		defaultValue = EntityHelper.IsDeleted.equals(fieldName) ? "F" : null;
		
		Field unsafeField = new FieldImpl(
				fieldName, physicalName, fieldLabel, entity, displayType.getFieldType(), CascadeModel.Ignore, maxLength, 
				nullableInDb, creatable, updatable, true, 8, defaultValue, autoValue);
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
		// 全英文直接返回
		if (text.matches("[a-zA-Z]+")) {
			return text;
		}
		
		String identifier = HanLP.convertToPinyinString(text, "", false);
		identifier = identifier.replaceAll("[^a-zA-Z0-9]", "");
		if (StringUtils.isBlank(identifier)) {
			throw new ModifiyMetadataException("无效名称 : " + text);
		}
		
		char start = identifier.charAt(0);
		if (!CharSet.ASCII_ALPHA.contains(start)) {
			identifier = "a" + identifier;
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
