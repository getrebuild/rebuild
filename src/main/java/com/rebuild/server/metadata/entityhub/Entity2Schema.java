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

package com.rebuild.server.metadata.entityhub;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.CascadeModel;
import cn.devezhao.persist4j.util.support.Table;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class Entity2Schema extends Field2Schema {

	private static final Log LOG = LogFactory.getLog(Entity2Schema.class);
	
	/**
	 * @param user
	 */
	public Entity2Schema(ID user) {
		super(user);
	}
	
	@Override
	public String create(Entity entity, String fieldLabel, DisplayType type, String comments, String refEntity) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param entityLabel
	 * @param comments
	 * @param masterEntity
	 * @return
	 */
	public String create(String entityLabel, String comments, String masterEntity) {
		return create(entityLabel, comments, masterEntity, false);
	}
	
	/**
	 * @param entityLabel
	 * @param comments
	 * @param masterEntity
	 * @param haveNameField
	 * @return 实体名称
	 */
	public String create(String entityLabel, String comments, String masterEntity, boolean haveNameField) {
		String entityName = toPinyinName(entityLabel);
		while (true) {
			if (MetadataHelper.containsEntity(entityName)) {
				entityName += (10 + RandomUtils.nextInt(89));
			} else {
				break;
			}
		}
		
		final boolean isSlave = StringUtils.isNotBlank(masterEntity);
		if (isSlave && !MetadataHelper.containsEntity(masterEntity)) {
			throw new ModificationMetadataException("无效主实体 : " + masterEntity);
		}
		
		String physicalName = "T__" + entityName.toUpperCase();
		
		Object maxTypeCode[] = Application.createQueryNoFilter(
				"select min(typeCode) from MetaEntity").unique();
		int typeCode = maxTypeCode == null || ObjectUtils.toInt(maxTypeCode[0]) == 0 
				? 999 : (ObjectUtils.toInt(maxTypeCode[0]) - 1);
		
		// 名称字段
		String nameFiled = EntityHelper.CreatedOn;
		if (haveNameField) {
			nameFiled = entityName + "Name";
		}
		
		Record record = EntityHelper.forNew(EntityHelper.MetaEntity, user);
		record.setString("entityLabel", entityLabel);
		record.setString("entityName", entityName);
		record.setString("physicalName", physicalName);
		record.setInt("typeCode", typeCode);
		if (StringUtils.isNotBlank(comments)) {
			record.setString("comments", comments);
		}
		if (isSlave) {
			record.setString("masterEntity", masterEntity);
		}
		record.setString("nameField", nameFiled);
		record = Application.getCommonService().create(record);
		tempMetaId.add(record.getPrimary());
		
		Entity tempEntity = new UnsafeEntity(entityName, physicalName, entityLabel, typeCode, nameFiled);
		try {
			String primaryFiled = entityName + "Id";
			createBuiltinField(tempEntity, primaryFiled, "ID", DisplayType.ID, null, null, null);
			// 自增ID
			createBuiltinField(tempEntity, EntityHelper.AutoId, "AUTOID", DisplayType.NUMBER, null, null, null);
			// 是否删除
			createBuiltinField(tempEntity, EntityHelper.IsDeleted, "ISDELETED", DisplayType.BOOL, null, null, null);
			
			if (haveNameField) {
				createField(tempEntity, nameFiled, entityLabel + "名称", DisplayType.TEXT, false, true, true, null, null, null, true);
			}
			
			createBuiltinField(tempEntity, EntityHelper.CreatedBy, "创建人", DisplayType.REFERENCE, null, "User", null);
			createBuiltinField(tempEntity, EntityHelper.CreatedOn, "创建时间", DisplayType.DATETIME, null, null, null);
			createBuiltinField(tempEntity, EntityHelper.ModifiedBy, "修改人", DisplayType.REFERENCE, null, "User", null);
			createBuiltinField(tempEntity, EntityHelper.ModifiedOn, "修改时间", DisplayType.DATETIME, null, null, null);
			
			// 明细实体关联字段
			// 明细实体无所属用户或部门，使用主实体的
			if (isSlave) {
				String masterLabel = EasyMeta.valueOf(masterEntity).getLabel();
				String masterField = masterEntity + "Id";
				createBuiltinField(tempEntity, masterField, masterLabel, DisplayType.REFERENCE, "引用主记录(" + masterLabel + ")", masterEntity, CascadeModel.Delete); 
			} else {
				// 助记码/搜索码
//				createBuiltinField(tempEntity, EntityHelper.QuickCode, "QUICKCODE", DisplayType.TEXT, null, null, null);
				createField(tempEntity, EntityHelper.QuickCode, "QUICKCODE", DisplayType.TEXT, true, false, false, null, null, null, true);
				
				createBuiltinField(tempEntity, EntityHelper.OwningUser, "所属用户", DisplayType.REFERENCE, null, "User", null);
				createBuiltinField(tempEntity, EntityHelper.OwningDept, "所属部门", DisplayType.REFERENCE, null, "Department", null);
			}
		} catch (Throwable ex) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[tempMetaId.size()]));
			return null;
		}
		
		boolean schemaReady = schema2Database(tempEntity);
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[tempMetaId.size()]));
			return null;
		}
		
		Application.getMetadataFactory().refresh(false);
		return entityName;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public boolean drop(Entity entity) {
		EasyMeta easyMeta = EasyMeta.valueOf(entity);
		ID metaRecordId = easyMeta.getMetaId();
		if (easyMeta.isBuiltin() || metaRecordId == null) {
			throw new ModificationMetadataException("系统内建实体不允许删除");
		}
		
		if (entity.getSlaveEntity() != null) {
			throw new ModificationMetadataException("不能删除主实体");
		}
		
		long count = 0;
		if ((count = checkRecordCount(entity)) > 0) {
			throw new ModificationMetadataException("不能删除有记录的实体 (" + entity.getName() + "=" + count + ")");
		}
		
		String ddl = String.format("drop table if exists `%s`", entity.getPhysicalName());
		try {
			Application.getSQLExecutor().execute(ddl);
		} catch (Throwable ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			return false;
		}
		
		Application.getBean(MetaEntityService.class).delete(metaRecordId);
		Application.getMetadataFactory().refresh(false);
		return true;
	}
	
	/**
	 * @param entity
	 * @param fieldName
	 * @param fieldLabel
	 * @param displayType
	 * @param comments
	 * @param refEntity
	 * @param cascade
	 * @return
	 */
	private Field createBuiltinField(Entity entity, String fieldName, String fieldLabel, DisplayType displayType, String comments,
			String refEntity, CascadeModel cascade) {
		comments = StringUtils.defaultIfBlank(comments, "系统内建");
		return createField(entity, fieldName, fieldLabel, displayType, false, false, false, comments, refEntity, cascade, false);
	}
	
	/**
	 * @param entity
	 * @return
	 */
	private boolean schema2Database(Entity entity) {
		Dialect dialect = Application.getPersistManagerFactory().getDialect();
		Table table = new Table(entity, dialect);
		String ddl[] = table.generateDDL(false, false);
		try {
			Application.getSQLExecutor().executeBatch(ddl);
		} catch (Throwable ex) {
			LOG.error("DDL Error : \n" + StringUtils.join(ddl, "\n"), ex);
			return false;
		}
		return true;
	}
}
