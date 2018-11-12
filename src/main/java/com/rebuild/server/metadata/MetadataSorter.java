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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.metadata.BaseMeta;

/**
 * 前端配置时的元数据过滤、排序
 * 
 * @author devezhao
 * @since 09/30/2018
 * @see EasyMeta
 */
public class MetadataSorter {
	
	private static final Log LOG = LogFactory.getLog(MetadataSorter.class);

	/**
	 * @return
	 */
	public static Entity[] sortEntities() {
		return sortEntities(false);
	}
	
	/**
	 * @param containsBizz
	 * @return
	 */
	public static Entity[] sortEntities(boolean containsBizz) {
		Entity[] entities = MetadataHelper.getEntities();
		sortBaseMeta(entities);
		
		List<Entity> list = new ArrayList<>();
		for (Entity entity : entities) {
			int ec = entity.getEntityCode();
			if (EasyMeta.isBuiltin(entity)  || (!containsBizz && isBizzFilter(ec))) {
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

		// 全部类型
		if (dtAllowed == null || dtAllowed.length == 0) {
			List<Field> list = new ArrayList<>();
			for (Field field : fields) {
				if (field.getType() != FieldType.PRIMARY) {
					list.add(field);
				}
			}
			return list.toArray(new Field[list.size()]);
		}
		
		List<Field> list = new ArrayList<>();
		for (Field field : fields) {
			DisplayType dtThat = EasyMeta.getDisplayType(field);
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
	 * 按 Label 首字母排序
	 * 
	 * @param metas
	 */
	public static void sortBaseMeta(BaseMeta[] metas) {
		Arrays.sort(metas, new Comparator<BaseMeta>() {
			@Override
			public int compare(BaseMeta foo, BaseMeta bar) {
				try {
					String fooLetter = PinyinHelper.convertToPinyinString(EasyMeta.getLabel(foo), "", PinyinFormat.WITHOUT_TONE).toLowerCase();
					String barLetter = PinyinHelper.convertToPinyinString(EasyMeta.getLabel(bar), "", PinyinFormat.WITHOUT_TONE).toLowerCase();
					return fooLetter.compareTo(barLetter);
				} catch (Exception e) {
					LOG.error(null, e);
					return 0;
				}
			}
		});
	}
	
	/**
	 * 设置时过滤某些 Bizz 实体的字段
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isBizzFilter(Field field) {
		int ec = field.getOwnEntity().getEntityCode();
		String fn = field.getName();
		if (ec == EntityHelper.User) {
			return "avatarUrl".equalsIgnoreCase(fn) || "password".equalsIgnoreCase(fn);
		}
		return false;
	}
	
	/**
	 * 是否 Bizz 实体
	 * 
	 * @param entityCode
	 * @return
	 */
	public static boolean isBizzFilter(int entityCode) {
		return entityCode == EntityHelper.User || entityCode == EntityHelper.Department || entityCode == EntityHelper.Role;
	}
}
