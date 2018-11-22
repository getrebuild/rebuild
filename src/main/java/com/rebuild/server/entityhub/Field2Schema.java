/*
rebuild - Building your system freely.
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

package com.rebuild.server.entityhub;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.CharSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.metadata.MetadataException;
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
	}
	
	/**
	 * @param entity
	 * @param fieldLabel
	 * @param type
	 * @param comments
	 * @param refEntity
	 * @return
	 */
	public String create(Entity entity, String fieldLabel, DisplayType type, String comments, String refEntity) {
		long count = 0;
		if ((count = checkRecordCount(entity)) > 50000) {
			throw new ModificationMetadataException("本实体记录过大，增加字段可能导致表损坏 (" + entity.getName() + "=" + count + ")");
		}
		
		String fieldName = toPinyinName(fieldLabel);
		while (true) {
			if (entity.containsField(fieldName)) {
				fieldName += (10 + RandomUtils.nextInt(89));
			} else {
				break;
			}
		}
		
		Field field = createField(entity, fieldName, fieldLabel, type, true, true, true, comments, refEntity, CascadeModel.Ignore);
		
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
	 * @param force
	 * @return
	 */
	public boolean drop(Field field, boolean force) {
		EasyMeta easyMeta = EasyMeta.valueOf(field);
		ID metaRecordId = easyMeta.getMetaId();
		if (easyMeta.isBuiltin() || metaRecordId == null) {
			throw new ModificationMetadataException("系统内建字段不允许删除");
		}
		
		Entity entity = field.getOwnEntity();
		if (force == false) {
			long count = 0;
			if ((count = checkRecordCount(entity)) > 50000) {
				throw new ModificationMetadataException("本实体记录过大，删除字段可能导致表损坏 (" + entity.getName() + "=" + count + ")");
			}
		}
		
		String ddl = String.format("alter table `%s` drop column `%s`", entity.getPhysicalName(), field.getPhysicalName());
		try {
			Application.getSQLExecutor().execute(ddl);
		} catch (Throwable ex) {
			LOG.error("DDL Error : \n" + ddl, ex);
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
	 * @param field
	 * @return
	 */
	private boolean schema2Database(Entity entity, Field field) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		StringBuilder ddl = new StringBuilder("alter table `" + entity.getPhysicalName() + "`\n  add column ");
		table.generateFieldDDL(field, ddl);
		try {
			Application.getSQLExecutor().executeBatch(new String[] { ddl.toString() });
		} catch (Throwable ex) {
			LOG.error("DDL Error : \n" + ddl, ex);
			return false;
		}
		return true;
	}
	
	/**
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
	 * @return
	 */
	protected Field createField(Entity entity, String fieldName, String fieldLabel, DisplayType displayType,
			boolean nullable, boolean creatable, boolean updatable, String comments, String refEntity, CascadeModel cascade) {
		Record record = EntityHelper.forNew(EntityHelper.MetaField, user);
		record.setString("belongEntity", entity.getName());
		record.setString("fieldName", fieldName);
		String physicalName = fieldName.toUpperCase();
		record.setString("physicalName", physicalName);
		record.setString("fieldLabel", fieldLabel);
		record.setString("displayType", displayType.name());
		record.setBoolean("nullable", nullable);
		record.setBoolean("creatable", creatable);
		record.setBoolean("updatable", updatable);
		if (StringUtils.isNotBlank(comments)) {
			record.setString("comments", comments);
		}
		if (displayType == DisplayType.PICKLIST) {
			refEntity = "PickList";
		}
		if (StringUtils.isNotBlank(refEntity)) {
			record.setString("refEntity", refEntity);
			if (cascade != null) {
				String cascadeAlias = cascade == CascadeModel.RemoveLinks ? "remove-links" : cascade.name().toLowerCase();
				record.setString("cascade", cascadeAlias);
			}
		}
		
		if (displayType == DisplayType.REFERENCE && StringUtils.isBlank(refEntity)) {
			throw new MetadataException("引用字段必须指定引用实体");
		}
		
		int maxLength = 767 / 2;
		if (displayType == DisplayType.FILE || displayType == DisplayType.IMAGE) {
			maxLength = 767;
		} else if (displayType == DisplayType.NTEXT) {
			maxLength = 65535;
		}
		
		record = Application.getCommonService().create(record);
		tempMetaId.add(record.getPrimary());
		
		Field unsafeField = new FieldImpl(
				fieldName, physicalName, fieldLabel, entity, displayType.getFieldType(), CascadeModel.Ignore, maxLength, 
				nullable, creatable, updatable, true, 6, null, false);
		if (entity instanceof UnsafeEntity) {
			((UnsafeEntity) entity).addField(unsafeField);
		}
		return unsafeField;
	}
	
	/**
	 * 中文 -> 拼音（去除空格）
	 * 
	 * @param text
	 * @return
	 */
	protected String toPinyinName(final String text) {
		String identifier = text;
		try {
			identifier = PinyinHelper.convertToPinyinString(text, "", PinyinFormat.WITHOUT_TONE);
			identifier = identifier.replaceAll("[^a-zA-Z0-9]", "");
		} catch (PinyinException e) {
			throw new MetadataException(text, e);
		}
		if (StringUtils.isBlank(identifier)) {
			throw new ModificationMetadataException("无效名称 : " + text);
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
			throw new ModificationMetadataException("无效名称 : " + text);
		}
		return identifier;
	}
}
