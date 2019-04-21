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

package com.rebuild.server.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;

/**
 * 前端配置时的元数据过滤、排序
 * 
 * @author devezhao
 * @since 09/30/2018
 * @see EasyMeta
 */
public class MetadataSorter {
	
	/**
	 * 全部实体
	 * 
	 * @return
	 */
	public static Entity[] sortEntities() {
		return sortEntities(null, true);
	}
	
	/**
	 * 用户权限内可用实体
	 * 
	 * @param user
	 * @return
	 */
	public static Entity[] sortEntities(ID user) {
		return sortEntities(user, false);
	}
	
	/**
	 * 用户权限内可用实体
	 * 
	 * @param user
	 * @param containsBizz 是否包括内建 BIZZ 实体
	 * @return
	 */
	public static Entity[] sortEntities(ID user, boolean containsBizz) {
		Entity[] entities = MetadataHelper.getEntities();
		sortBaseMeta(entities);
		
		List<Entity> list = new ArrayList<>();
		for (Entity entity : entities) {
			int ec = entity.getEntityCode();
			if (EasyMeta.valueOf(ec).isBuiltin()) {
				if (containsBizz && MetadataHelper.isBizzEntity(ec)) {
					list.add(entity);
				}
			} else if (user == null) {
				list.add(entity);
			} else if (Application.getSecurityManager().allowedR(user, ec)) {
				list.add(entity);
			}
		}
		return list.toArray(new Entity[list.size()]);
	}
	
	/**
	 * 获取指定类型字段
	 * 
	 * @param entity
	 * @param allowed 指定的类型
	 * @return
	 */
	public static Field[] sortFields(Entity entity, DisplayType... allowed) {
		return sortFields(entity.getFields(), allowed);
	}
	
	/**
	 * 字段排序，同时可过滤字段（根据类型）
	 * 
	 * @param fields
	 * @param allowed
	 * @return
	 */
	public static Field[] sortFields(Field[] fields, DisplayType... allowed) {
		List<Field> sysFields = new ArrayList<>();
		List<Field> simpleFields = new ArrayList<>();
		for (Field field : fields) {
			if (EasyMeta.valueOf(field).isBuiltin()) {
				sysFields.add(field);
			} else {
				simpleFields.add(field);
			}
		}
		
		// 系统字段在后
		Field[] sysFieldsAry = sysFields.toArray(new Field[sysFields.size()]);
		Field[] simpleFieldsAry = simpleFields.toArray(new Field[simpleFields.size()]);
		sortBaseMeta(sysFieldsAry);
		sortBaseMeta(simpleFieldsAry);
		Field[] allFields = (Field[]) ArrayUtils.addAll(simpleFieldsAry, sysFieldsAry);

		// 返回全部类型
		if (allowed == null || allowed.length == 0) {
			List<Field> list = new ArrayList<>();
			for (Field field : allFields) {
				if (!MetadataHelper.isSystemField(field)) {
					list.add(field);
				}
			}
			return list.toArray(new Field[list.size()]);
		}
		
		List<Field> list = new ArrayList<>();
		for (Field field : allFields) {
			DisplayType dtThat = EasyMeta.getDisplayType(field);
			for (DisplayType dt : allowed) {
				if (dtThat.equals(dt)) {
					list.add(field);
					break;
				}
			}
		}
		return list.toArray(new Field[list.size()]);
	}
	
	/**
	 * 按 Label 排序
	 * 
	 * @param metas
	 */
	private static void sortBaseMeta(BaseMeta[] metas) {
		Arrays.sort(metas, new Comparator<BaseMeta>() {
			@Override
			public int compare(BaseMeta foo, BaseMeta bar) {
				String fooLetter = EasyMeta.getLabel(foo);
				String barLetter = EasyMeta.getLabel(bar);
				return fooLetter.compareTo(barLetter);
			}
		});
	}
}
