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

import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.AccessibleMeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.metadata.BaseMeta;

/**
 * 前端配置时的元数据过滤
 * 
 * @author devezhao
 * @since 09/30/2018
 * @see AccessibleMeta
 */
public class PortalMetaSorter {

	/**
	 * @return
	 */
	public static Entity[] sortEntities() {
		return sortEntities(false);
	}
	
	/**
	 * @param fromAdmin
	 * @return
	 */
	public static Entity[] sortEntities(boolean fromAdmin) {
		Entity[] entities = MetadataHelper.getEntities();
		sortBaseMeta(entities);
		
		List<Entity> list = new ArrayList<>();
		for (Entity entity : entities) {
			int ec = entity.getEntityCode();
			if (AccessibleMeta.isBuiltin(entity) 
					|| (!fromAdmin && (ec == EntityHelper.User || ec == EntityHelper.Department || ec == EntityHelper.Role))) {
			} else {
				list.add(entity);
			}
		}
		return list.toArray(new Entity[list.size()]);
	}
	
	/**
	 * @param entity
	 * @return
	 */
	public static Field[] sortFields(Entity entity) {
		return sortFields(entity.getFields(), null);
	}
	
	/**
	 * @param entity
	 * @param dtAllowed
	 * @return
	 */
	public static Field[] sortFields(Entity entity, DisplayType[] dtAllowed) {
		return sortFields(entity.getFields(), dtAllowed);
	}
	
	/**
	 * @param fields
	 * @param dtAllowed
	 * @return
	 */
	public static Field[] sortFields(Field[] fields, DisplayType[] dtAllowed) {
		sortBaseMeta(fields);
		if (dtAllowed == null || dtAllowed.length == 0) {
			List<Field> list = new ArrayList<>();
			for (Field field : fields) {
				if (field.getType() == FieldType.PRIMARY) {
					continue;
				}
				list.add(field);
			}
			return list.toArray(new Field[list.size()]);
		}
		
		List<Field> list = new ArrayList<>();
		for (Field field : fields) {
			DisplayType dtThat = AccessibleMeta.getDisplayType(field);
			for (DisplayType dt : dtAllowed) {
				if (dtThat == dt) {
					list.add(field);
					break;
				}
			}
		}
		return list.toArray(new Field[list.size()]);
	}
	
	/**
	 * 排序
	 * 
	 * @param metas
	 */
	private static void sortBaseMeta(BaseMeta[] metas) {
//		ArrayUtils.reverse(metas);
		
		// TODO 元数据排序算法
		
//		Arrays.sort(metas, new Comparator<BaseMeta>() {
//			@Override
//			public int compare(BaseMeta a, BaseMeta b) {
//				int c = EasyMeta.getLabel(a).compareToIgnoreCase(EasyMeta.getLabel(b));
//				return c;
//			}
//		});
	}
}
