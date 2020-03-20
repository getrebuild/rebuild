/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
		List<Entity> sorted = new ArrayList<>();

		// 排序在前
		if (containsBizz) {
			sorted.add(MetadataHelper.getEntity(EntityHelper.User));
			sorted.add(MetadataHelper.getEntity(EntityHelper.Department));
			sorted.add(MetadataHelper.getEntity(EntityHelper.Role));
			sorted.add(MetadataHelper.getEntity(EntityHelper.Team));
		}

		Entity[] entities = MetadataHelper.getEntities();
		sortBaseMeta(entities);
		for (Entity e : entities) {
			if (EasyMeta.valueOf(e).isBuiltin() && !MetadataHelper.hasPrivilegesField(e)) {
				if (!MetadataHelper.isPlainEntity(e.getEntityCode())) {
					continue;
				}
			}

			if (user == null) {
				sorted.add(e);
			} else if (Application.getSecurityManager().allowRead(user, e.getEntityCode())) {
				sorted.add(e);
			}
		}
		return sorted.toArray(new Entity[0]);
	}
	
	/**
	 * 获取字段
	 * 
	 * @param entity
	 * @param allowedTypes
	 * @return
	 * @see #sortFields(Field[], DisplayType...)
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
			} else if (MetadataHelper.isCommonsField(field)) {
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
