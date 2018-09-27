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

import com.rebuild.server.Application;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.MetadataException;
import cn.devezhao.persist4j.metadata.MetadataFactory;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class MetadataHelper {

	/**
	 * @return
	 */
	public static MetadataFactory getMetadataFactory() {
		return Application.getPersistManagerFactory().getMetadataFactory();
	}
	
	/**
	 * 更新元数据缓存
	 */
	public static void refreshMetadata() {
		((ConfigurationMetadataFactory) getMetadataFactory()).refresh(false);
	}
	
	/**
	 * 获取实体扩展信息
	 * 
	 * @param entity
	 * @return
	 */
	public static Object[] getEntityExtmeta(Entity entity) {
		return ((DynamicMetadataFactory) getMetadataFactory())
				.getEntityExtmeta(entity.getName());
	}
	
	/**
	 * 获取字段扩展信息
	 * 
	 * @param field
	 * @return
	 */
	public static Object[] getFieldExtmeta(Field field) {
		return ((DynamicMetadataFactory) getMetadataFactory())
				.getFieldExtmeta(field.getOwnEntity().getName(), field.getName());
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
}
