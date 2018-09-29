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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.StringHelper;
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
	 * @return
	 */
	public String create(String entityLabel, String comments) {
		String entityName = toPinyinString(entityLabel);
		while (true) {
			Object exists = Application.createNoFilterQuery(
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
		
		Object maxTypeCode[] = Application.createNoFilterQuery(
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
		record.setString("nameField", EntityHelper.createdOn);
		record = Application.getCommonService().create(record);
		tempMetaId.add(record.getPrimary());
		
		Entity unsafeEntity = new UnsafeEntity(entityName, physicalName, entityLabel, typeCode, EntityHelper.createdOn);
		
		String primaryFiled = entityName + "Id";
		createField(unsafeEntity, primaryFiled, "ID", DisplayType.ID, false, false, false, null, "系统内建");
		createField(unsafeEntity, EntityHelper.createdBy, "创建人", DisplayType.REFERENCE, false, false, false, "User", "系统内建");
		createField(unsafeEntity, EntityHelper.createdOn, "创建时间", DisplayType.DATETIME, false, false, false, null, "系统内建");
		createField(unsafeEntity, EntityHelper.modifiedBy, "修改人", DisplayType.REFERENCE, false, false, true, "User", "系统内建");
		createField(unsafeEntity, EntityHelper.modifiedOn, "修改时间", DisplayType.DATETIME, false, false, true, null, "系统内建");
		createField(unsafeEntity, EntityHelper.owningUser, "所属用户", DisplayType.REFERENCE, false, false, true, "User", "系统内建");
		createField(unsafeEntity, EntityHelper.owningDept, "所属部门", DisplayType.REFERENCE, false, false, true, "Department", "系统内建");
		
		boolean schemaReady = schema2Database(unsafeEntity);
		if (!schemaReady) {
			Application.getCommonService().delete(tempMetaId.toArray(new ID[tempMetaId.size()]));
			return null;
		}
		
		MetadataHelper.refreshMetadata();
		return entityName;
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
}
