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

import com.rebuild.server.Application;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
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
	 * @param entityName
	 * @param fieldName
	 * @return
	 */
	public static Field getField(String entityName, String fieldName) {
		Entity entity = getEntity(entityName);
		return entity.getField(fieldName);
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
			if (ref.getEntityCode() == source.getEntityCode()) {
				fields.add(field);
			}
		}
		return fields.toArray(new Field[fields.size()]);
	}
}
