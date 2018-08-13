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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

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
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.ExtRecordCreator;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class Entity2Schema {

	private static final Log LOG = LogFactory.getLog(Entity2Schema.class);
	
	final private ID user;
	
	/**
	 * @param user
	 */
	public Entity2Schema(ID user) {
		this.user = user;
	}
	
	/**
	 * @param entityLabel
	 * @param comments
	 * @return
	 */
	public String create(String entityLabel, String comments) {
		String entityName = toPinyinString(entityLabel);
		while (true) {
			Object exists = Application.createQuery(
					"select entityId from MetaEntity where entityName = ?")
					.setParameter(1, entityName)
					.unique();
			if (exists != null) {
				entityName += (1000 + RandomUtils.nextInt(8999));
			} else {
				break;
			}
		}
		String physicalName = "T__" + StringHelper.hyphenate(entityName).toUpperCase();
		
		Object maxTypeCode[] = Application.createQuery(
				"select min(typeCode) from MetaEntity").unique();
		int typeCode = maxTypeCode == null || ObjectUtils.toInt(maxTypeCode[0]) == 0 
				? 999 : (ObjectUtils.toInt(maxTypeCode[0]) - 1);
		
		Record record = EntityHelper.forNew(EntityHelper.MetaEntity, user);
		record.setString("entityLabel", entityLabel);
		record.setString("entityName", entityName);
		record.setString("physicalName", physicalName);
		record.setInt("typeCode", typeCode);
		if (StringUtils.isNotBlank(comments)) {
			record.setString("comments", comments);
		}
		record = Application.getCommonService().create(record);
		ID metaEntityId = record.getPrimary();
		
		Entity unsafeEntity = new UnsafeEntity(entityName, physicalName, entityLabel, typeCode, null);
		
		String primaryFiled = entityName + "Id";
		createField(unsafeEntity, metaEntityId, primaryFiled, "ID", DisplayType.ID, false, false, false, null, null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.createdOn, "创建人", DisplayType.REFERENCE, false, false, false, "User", null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.createdBy, "创建时间", DisplayType.DATETIME, false, false, false, null, null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.modifiedBy, "修改人", DisplayType.REFERENCE, false, false, true, "User", null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.modifiedOn, "修改时间", DisplayType.DATETIME, false, false, true, null, null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.owningUser, "所属用户", DisplayType.REFERENCE, false, false, true, "User", null);
		createField(unsafeEntity, metaEntityId, ExtRecordCreator.owningDept, "所属部门", DisplayType.REFERENCE, false, false, true, "Department", null);
		
		boolean schemaReady = schema2Database(unsafeEntity);
		if (!schemaReady) {
			Application.getCommonService().delete(metaEntityId);
			return null;
		}
		
		MetadataHelper.refreshMetadata();
		return entityName;
	}
	
	/**
	 * @param unsafeEntity
	 * @param entityId
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
	protected ID createField(Entity unsafeEntity, ID entityId, String fieldName, String fieldLabel, DisplayType displayType,
			boolean nullable, boolean creatable, boolean updatable, String refEntity, String comments) {
		Record record = EntityHelper.forNew(EntityHelper.MetaField, user);
		record.setID("entityId", entityId);
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
		if (StringUtils.isNotBlank(refEntity)) {
			record.setString("refEntity", refEntity);
		}
		
		record = Application.getCommonService().create(record);
		
		Field unsafeField = new FieldImpl(fieldName, physicalName, fieldLabel, unsafeEntity, displayType.getFieldType(), CascadeModel.Ignore, 0, nullable, updatable, 0, null, false);
		((UnsafeEntity) unsafeEntity).addField(unsafeField);
		
		return record.getPrimary();
	}
	
	/**
	 * @param entity
	 * @return
	 */
	private boolean schema2Database(Entity entity) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		String ddlSqls[] = table.generateDDL(false, false);
		try {
			Application.getSqlExecutor().executeBatch(ddlSqls);
		} catch (Throwable ex) {
			LOG.error("DDL Error : \n" + StringUtils.join(ddlSqls, "\n"), ex);
			return false;
		}
		return true;
	}
	
	// 中文 -> 拼音
	// 去除空格
	private String toPinyinString(String text) {
		try {
			text = PinyinHelper.convertToPinyinString(text, "", PinyinFormat.WITHOUT_TONE);
			text = StringUtils.trimToEmpty(text);
			text = text.replace(" ", "");
			return text;
		} catch (PinyinException e) {
			throw new RuntimeException(text, e);
		}
	}
}
