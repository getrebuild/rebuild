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

package com.rebuild.server.metadata;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataException;

/**
 * 实体元数据
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class MetadataHelper {

	/**
	 * 元数据
	 * 
	 * @return
	 */
	public static DynamicMetadataFactory getMetadataFactory() {
		return (DynamicMetadataFactory) Application.getPersistManagerFactory().getMetadataFactory();
	}
	
	/**
	 * 获取实体扩展信息
	 * 
	 * @param entity
	 * @return
	 */
	public static Object[] getEntityExtmeta(Entity entity) {
		return getMetadataFactory().getEntityExtmeta(entity.getName());
	}
	
	/**
	 * 获取字段扩展信息
	 * 
	 * @param field
	 * @return
	 */
	public static Object[] getFieldExtmeta(Field field) {
		return getMetadataFactory().getFieldExtmeta(field.getOwnEntity().getName(), field.getName());
	}
	
	/**
	 * @return
	 */
	public static Entity[] getEntities() {
		return getMetadataFactory().getEntities();
	}
	
	/**
	 * @param entityName
	 * @return
	 */
	public static boolean containsEntity(String entityName) {
		try {
			getEntity(entityName);
			return true;
		} catch (MetadataException ex) {
			return false;
		}
	}
	
	/**
	 * @param entityName
	 * @return
	 */
	public static Entity getEntity(String entityName) {
		return getMetadataFactory().getEntity(entityName);
	}

	/**
	 * @param entityCode
	 * @return
	 */
	public static Entity getEntity(int entityCode) {
		return getMetadataFactory().getEntity(entityCode);
	}
	
	/**
	 * @param record
	 * @return
	 */
	public static String getEntityName(ID record) {
		return getMetadataFactory().getEntity(record.getEntityCode()).getName();
	}
	
	/**
	 * @param entityName
	 * @param fieldName
	 * @return
	 */
	public static Field getField(String entityName, String fieldName) {
		Entity entity = getEntity(entityName);
		return entity.getField(fieldName);
	}
	
	/**
	 * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
	 * 
	 * @param entity
	 * @return
	 */
	public static Field getNameField(Entity entity) {
		Field hasName = entity.getNameField();
		if (hasName != null) {
			return hasName;
		}
		if (entity.containsField(EntityHelper.CreatedOn)) {
			return entity.getField(EntityHelper.CreatedOn);
		}
		return entity.getPrimaryField();
	}
	
	/**
	 * {@link Entity#getNameField()} 有可能返回空，应优先使用此方法
	 * 
	 * @param entity
	 * @return
	 */
	public static Field getNameField(String entity) {
		return getNameField(getEntity(entity));
	}
	
	/**
	 * <tt>reference</tt> 中的哪些字段引用了 <tt>source</tt>
	 * 
	 * @param source
	 * @param reference
	 * @return
	 */
	public static Field[] getReferenceToFields(Entity source, Entity reference) {
		List<Field> fields = new ArrayList<>();
		for (Field field : reference.getFields()) {
			if (field.getType() != FieldType.REFERENCE) {
				continue;
			}
			
			Entity ref = field.getReferenceEntities()[0];
			if (ref.getEntityCode().equals(source.getEntityCode())) {
				fields.add(field);
			}
		}
		return fields.toArray(new Field[fields.size()]);
	}
	
	/**
	 * 设置时过滤某些 Bizz 实体的字段
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isFilterField(Field field) {
		int ec = field.getOwnEntity().getEntityCode();
		String fn = field.getName();
		if (ec == EntityHelper.User) {
			return "avatarUrl".equalsIgnoreCase(fn) || "password".equalsIgnoreCase(fn);
		}
		return false;
	}
	
	/**
	 * 仅供系统使用的字段
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isSystemField(Field field) {
		String fieldName = field.getName();
		if (EntityHelper.AutoId.equalsIgnoreCase(fieldName)
				|| EntityHelper.QuickCode.equalsIgnoreCase(fieldName)
				|| EntityHelper.IsDeleted.equalsIgnoreCase(fieldName)) {
			return true;
		}
		if (field.getType() == FieldType.PRIMARY) {
			return true;
		}
		return false;
	}
	
	/**
	 * 是否 Bizz 实体
	 * 
	 * @param entityCode
	 * @return
	 */
	public static boolean isBizzEntity(int entityCode) {
		return entityCode == EntityHelper.User || entityCode == EntityHelper.Department || entityCode == EntityHelper.Role;
	}
	
	/**
	 * 获取内建实体的 DisplayType
	 * 
	 * @param field
	 * @return
	 */
	public static DisplayType getBuiltinFieldType(Field field) {
		int ec = field.getOwnEntity().getEntityCode();
		String fn = field.getName();
		if (ec == EntityHelper.User && "email".equals(fn)) {
			return DisplayType.EMAIL;
		}
		
		Type ft = field.getType();
		if (ft == FieldType.PRIMARY) {
			return DisplayType.ID;
		} else if (ft == FieldType.REFERENCE) {
			return DisplayType.REFERENCE;
		} else if (ft == FieldType.TIMESTAMP) {
			return DisplayType.DATETIME;
		} else if (ft == FieldType.DATE) {
			return DisplayType.DATE;
		} else if (ft == FieldType.STRING) {
			return DisplayType.TEXT;
		} else if (ft == FieldType.BOOL) {
			return DisplayType.BOOL;
		} else if (ft == FieldType.INT || ft == FieldType.SMALL_INT) {
			return DisplayType.NUMBER;
		} else if (ft == FieldType.TEXT) {
			return DisplayType.NTEXT;
		}
		return null;
	}
	
	/**
	 * 获取明细实体哪个字段引用自主实体
	 * 
	 * @param slave
	 * @return
	 */
	public static Field getSlaveToMasterField(Entity slave) {
		Entity master = slave.getMasterEntity();
		Assert.isTrue(master != null, "Non slave entity");
		
		for (Field field : slave.getFields()) {
			if (field.getType() != FieldType.REFERENCE) {
				continue;
			}
			// 内建的那个才是，因为明细的其他字段也可能引用主实体
			if (master.equals(field.getReferenceEntity()) && EasyMeta.valueOf(field).isBuiltin()) {
				return field;
			}
		}
		return null;
	}
	
	/**
	 * 是否主实体
	 * 
	 * @param entityCode
	 * @return
	 */
	public static boolean isMasterEntity(int entityCode) {
		return getEntity(entityCode).getSlaveEntity() != null;
	}
	
	/**
	 * 是否明细实体
	 * 
	 * @param entityCode
	 * @return
	 */
	public static boolean isSlaveEntity(int entityCode) {
		return getEntity(entityCode).getMasterEntity() != null;
	}
}
