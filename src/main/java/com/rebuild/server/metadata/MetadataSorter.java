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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据辅助类，支持过滤/排序字段或实体
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
	 * 用户权限内可见实体（具备读取权限）
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
			if (EasyMeta.valueOf(entity).isBuiltin()) {
				if (containsBizz && MetadataHelper.isBizzEntity(entity.getEntityCode())) {
					list.add(entity);
				}
			} else if (user == null) {
				list.add(entity);
			} else if (Application.getSecurityManager().allowedR(user, entity.getEntityCode())) {
				list.add(entity);
			}
		}

		// 内建业务实体
		for (Entity entity : entities) {
			if (EasyMeta.valueOf(entity).isBuiltin() && MetadataHelper.hasPrivilegesField(entity)) {
				list.add(entity);
			}
		}

		return list.toArray(new Entity[0]);
	}
	
	/**
	 * 获取字段
	 * 
	 * @param entity
	 * @param allowedTypes 仅返回指定的类型
	 * @return
	 */
	public static Field[] sortFields(Entity entity, DisplayType... allowedTypes) {
		return sortFields(entity.getFields(), allowedTypes);
	}
	
	/**
	 * 获取字段
	 * 
	 * @param fields
	 * @param allowedTypes 仅返回指定的类型
	 * @return
	 */
	public static Field[] sortFields(Field[] fields, DisplayType... allowedTypes) {
		List<Field> othersFields = new ArrayList<>();
		List<Field> commonsFields = new ArrayList<>();
		List<Field> approvalFields = new ArrayList<>();
		for (Field field : fields) {
			if (MetadataHelper.isApprovalField(field.getName())) {
				approvalFields.add(field);
			} else if (EasyMeta.valueOf(field).isBuiltin()) {
				commonsFields.add(field);
			} else {
				othersFields.add(field);
			}
		}
		
		Field[] allFields = othersFields.toArray(new Field[0]);
		sortBaseMeta(allFields);
		// 公共字段在后
		Field[] commonsFieldsAry = commonsFields.toArray(new Field[0]);
		sortBaseMeta(commonsFieldsAry);
		allFields = (Field[]) ArrayUtils.addAll(allFields, commonsFieldsAry);
		// 审批字段在后
		if (!approvalFields.isEmpty()) {
			Field[] approvalFieldsAry = approvalFields.toArray(new Field[0]);
			sortBaseMeta(approvalFieldsAry);
			allFields = (Field[]) ArrayUtils.addAll(allFields, approvalFieldsAry);
		}

		// 返回全部
		if (allowedTypes == null || allowedTypes.length == 0) {
			List<Field> list = new ArrayList<>();
			for (Field field : allFields) {
				if (!MetadataHelper.isSystemField(field)) {
					list.add(field);
				}
			}
			return list.toArray(new Field[0]);
		}
		
		List<Field> list = new ArrayList<>();
		for (Field field : allFields) {
			DisplayType dtThat = EasyMeta.getDisplayType(field);
			for (DisplayType dt : allowedTypes) {
				if (dtThat.equals(dt) && !MetadataHelper.isSystemField(field)) {
					list.add(field);
					break;
				}
			}
		}
		return list.toArray(new Field[0]);
	}
	
	/**
	 * 按 Label 排序
	 * 
	 * @param metas
	 */
	private static void sortBaseMeta(BaseMeta[] metas) {
		Arrays.sort(metas, (foo, bar) -> {
			String fooLetter = EasyMeta.getLabel(foo);
			String barLetter = EasyMeta.getLabel(bar);
			return fooLetter.compareTo(barLetter);
		});
	}
}
