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
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 创建实体
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
	
	/**
	 * @param entityLabel
	 * @param comments
	 * @param masterEntity
	 * @param haveNameField
	 * @return
	 */
	public String createEntity(String entityLabel, String comments, String masterEntity, boolean haveNameField) {
		return createEntity(null, entityLabel, comments, masterEntity, haveNameField);
	}
	
	/**
	 * @param entityName
	 * @param entityLabel
	 * @param comments
	 * @param masterEntity
	 * @param haveNameField
	 * @return returns 实体名称
	 */
	public String createEntity(String entityName, String entityLabel, String comments, String masterEntity, boolean haveNameField) {
		if (entityName != null) {
			if (MetadataHelper.containsEntity(entityName)) {
				throw new ModifiyMetadataException("重复实体名称 : " + entityName); 
			}
		} else {
			entityName = toPinyinName(entityLabel);
			while (true) {
				if (MetadataHelper.containsEntity(entityName)) {
					entityName += (100 + RandomUtils.nextInt(900));
				} else {
					break;
				}
			}
		}
		
		final boolean isSlave = StringUtils.isNotBlank(masterEntity);
		if (isSlave && !MetadataHelper.containsEntity(masterEntity)) {
			throw new ModifiyMetadataException("无效主实体 : " + masterEntity);
		}
		
		String physicalName = "T__" + entityName.toUpperCase();
		
		Object maxTypeCode[] = Application.createQueryNoFilter(
				"select min(typeCode) from MetaEntity").unique();
		int typeCode = maxTypeCode == null || ObjectUtils.toInt(maxTypeCode[0]) == 0 
				? 999 : (ObjectUtils.toInt(maxTypeCode[0]) - 1);
		if (typeCode <= 200) {
			throw new ModifiyMetadataException("Entity code exceeds system limit : " + typeCode);
		}
		
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
			
			if (haveNameField) {
				createUnsafeField(
						tempEntity, nameFiled, entityLabel + "名称", DisplayType.TEXT, false, true, true, true,null, null, null, null, null);
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
				createUnsafeField(
						tempEntity, EntityHelper.QuickCode, "QUICKCODE", DisplayType.TEXT, true, false, false, true,null, null, null, null, null);
				
				createBuiltinField(tempEntity, EntityHelper.OwningUser, "所属用户", DisplayType.REFERENCE, null, "User", null);
				createBuiltinField(tempEntity, EntityHelper.OwningDept, "所属部门", DisplayType.REFERENCE, null, "Department", null);
			}
		} catch (Throwable ex) {
		    LOG.error(null, ex);
			Application.getCommonService().delete(tempMetaId.toArray(new ID[0]));
			return null;
		}
		
		boolean schemaReady = schema2Database(tempEntity);
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[0]));
			return null;
		}
		
		Application.getMetadataFactory().refresh(false);
		return entityName;
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public boolean dropEntity(Entity entity) {
		return dropEntity(entity, false);
	}
	
	/**
	 * @param entity
	 * @param force
	 * @return
	 */
	public boolean dropEntity(Entity entity, boolean force) {
		if (!user.equals(UserService.ADMIN_USER)) {
			throw new ModifiyMetadataException("仅超级管理员可删除实体");
		}
		
		EasyMeta easyMeta = EasyMeta.valueOf(entity);
		ID metaRecordId = easyMeta.getMetaId();
		if (easyMeta.isBuiltin() || metaRecordId == null) {
			throw new ModifiyMetadataException("系统内建实体不允许删除");
		}
		
		if (entity.getSlaveEntity() != null) {
			throw new ModifiyMetadataException("不能删除主实体");
		}
		
		if (!force) {
			for (Field whoRef : entity.getReferenceToFields(true)) {
				if (!whoRef.getOwnEntity().equals(entity)) {
					throw new ModifiyMetadataException("实体已被引用 (引用实体: " + EasyMeta.getLabel(whoRef.getOwnEntity()) + ")");
				}
			}

			long count = 0;
			if ((count = checkRecordCount(entity)) > 0) {
				throw new ModifiyMetadataException("不能删除有数据的实体 (数量: " + count + ")");
			}
		}
		
		String ddl = String.format("drop table if exists `%s`", entity.getPhysicalName());
		try {
			Application.getSQLExecutor().execute(ddl, 10 * 60);
		} catch (Throwable ex) {
			LOG.error("DDL ERROR : \n" + ddl, ex);
			return false;
		}
		
		final ID sessionUser = Application.getSessionStore().get(true);
		if (sessionUser == null) {
			Application.getSessionStore().set(user);
		}
		try {
			Application.getBean(MetaEntityService.class).delete(metaRecordId);
		} finally {
			if (sessionUser == null) {
				Application.getSessionStore().clean();
			}
		}
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
		return createUnsafeField(
				entity, fieldName, fieldLabel, displayType, false, false, false, true, comments, refEntity, cascade, null, null);
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
