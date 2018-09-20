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

package cn.devezhao.rebuild.server.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.rebuild.server.Application;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class MetadataHelper {

	/**
	 * 更新元数据缓存
	 */
	public static void refreshMetadata() {
		((ConfigurationMetadataFactory) Application.getMetadataFactory()).refresh(false);
	}
	
	/**
	 * 获取实体扩展信息
	 * 
	 * @param entity
	 * @return
	 */
	public static Object[] getEntityExtmeta(Entity entity) {
		return ((DynamicMetadataFactory) Application.getMetadataFactory())
				.getEntityExtmeta(entity.getName());
	}
	
	/**
	 * 获取字段扩展信息
	 * 
	 * @param field
	 * @return
	 */
	public static Object[] getFieldExtmeta(Field field) {
		return ((DynamicMetadataFactory) Application.getMetadataFactory())
				.getFieldExtmeta(field.getOwnEntity().getName(), field.getName());
	}
	
	/**
	 * @return
	 */
	public static Entity[] getEntities() {
		return Application.getMetadataFactory().getEntities();
	}
	
	/**
	 * @param entityName
	 * @return
	 */
	public static Entity getEntity(String entityName) {
		return Application.getPersistManagerFactory().getMetadataFactory().getEntity(entityName);
	}

	/**
	 * @param entityCode
	 * @return
	 */
	public static Entity getEntity(int entityCode) {
		return Application.getPersistManagerFactory().getMetadataFactory().getEntity(entityCode);
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
