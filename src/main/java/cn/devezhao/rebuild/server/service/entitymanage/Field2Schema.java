/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entitymanage;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

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
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;

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
		String fieldName = toPinyinString(fieldLabel);
		while (true) {
			if (entity.containsField(fieldName)) {
				fieldName += (1000 + RandomUtils.nextInt(8999));
			} else {
				break;
			}
		}
		
		Field field = createField(entity, fieldName, fieldLabel, type, true, true, true, refEntity, comments);
		
		boolean schemaReady = schema2Database(entity, field);
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[tempMetaId.size()]));
			return null;
		}
		
		MetadataHelper.refreshMetadata();
		return fieldName;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private boolean schema2Database(Entity entity, Field field) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		StringBuilder ddlSql = new StringBuilder("alter table `" + entity.getPhysicalName() + "`\n  add column ");
		table.generateFieldDDL(field, ddlSql);
		try {
			Application.getSqlExecutor().executeBatch(new String[] { ddlSql.toString() });
		} catch (Throwable ex) {
			LOG.error("DDL Error : \n" + ddlSql, ex);
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
	 * @param refEntity
	 * @param comments
	 * @return
	 */
	protected Field createField(Entity entity, String fieldName, String fieldLabel, DisplayType displayType,
			boolean nullable, boolean creatable, boolean updatable, String refEntity, String comments) {
		Record record = EntityHelper.forNew(EntityHelper.MetaField, user);
		record.setString("belongEntity", entity.getName());
		record.setString("fieldName", fieldName);
		String physicalName = StringHelper.hyphenate(fieldName).toUpperCase();
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
		}
		
		if (displayType == DisplayType.REFERENCE && StringUtils.isBlank(refEntity)) {
			throw new MetadataException("引用字段必须指定引用实体");
		}
		
		record = Application.getCommonService().create(record);
		tempMetaId.add(record.getPrimary());
		
		Field unsafeField = new FieldImpl(fieldName, physicalName, fieldLabel, entity, displayType.getFieldType(), CascadeModel.Ignore, 600, nullable, updatable, 6, null, false);
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
	protected String toPinyinString(String text) {
		String identifier = null;
		try {
			text = PinyinHelper.convertToPinyinString(text, "", PinyinFormat.WITHOUT_TONE);
			text = StringUtils.trimToEmpty(text);
			text = text.replace(" ", "");
			identifier = text.toLowerCase();
		} catch (PinyinException e) {
			throw new MetadataException(text, e);
		}
		
		if (identifier.length() > 40) {
			identifier = identifier.substring(0, 40);
		}
		if (!StringHelper.isIdentifier(identifier)) {
			throw new MetadataException("无效名称 : " + text);
		}
		return identifier;
	}
}
