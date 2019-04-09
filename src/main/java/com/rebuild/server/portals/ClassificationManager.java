/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.portals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 分类数据。TODO 缓存
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/28
 */
public class ClassificationManager implements PortalsManager {

	private static final Log LOG = LogFactory.getLog(ClassificationManager.class);
	
	/**
	 * 获取名称
	 * 
	 * @param itemId
	 * @return
	 */
	public static String getName(ID itemId) {
		Object[] o = Application.createQueryNoFilter(
				"select name from ClassificationData where itemId = ?")
				.setParameter(1, itemId)
				.unique();
		return o == null ? null : (String) o[0];
	}
	
	/**
	 * 获取全名称（包括父级，用 . 分割）
	 * 
	 * @param itemId
	 * @return
	 */
	public static String getFullName(ID itemId) {
		List<String> names = new ArrayList<>();
		while (itemId != null) {
			Object[] o = Application.createQueryNoFilter(
					"select name,parent from ClassificationData where itemId = ?")
					.setParameter(1, itemId)
					.unique();
			names.add((String) o[0]);
			itemId = (ID) o[1];
		}
		
		String namesArr[] = names.toArray(new String[names.size()]);
		ArrayUtils.reverse(namesArr);
		return StringUtils.join(namesArr, ".");
	}
	
	/**
	 * 从最后一级开始查找，最多向上匹配两级
	 * TODO 更优的查询方式
	 * 
	 * @param name
	 * @param field
	 * @return
	 */
	public static ID findByName(String name, Field field) {
		ID dataId = getUseClassification(field);
		if (dataId == null) {
			return null;
		}
		
		String[] names = name.split("\\.");
		String baseSql = String.format("select itemId,parent,parent.name from ClassificationData where dataId = '%s' and name = ?", dataId);
		
		Object[][] hasMany = Application.createQueryNoFilter(baseSql)
			.setParameter(1, names[names.length - 1])
			.array();
		if (hasMany.length == 1) {
			ID itemId = (ID) hasMany[0][0];
			return itemId;
		} else if (hasMany.length == 0) {
			return null;
		}
		
		if (names.length < 2) {
			return null;
		}
		
		// 有多个匹配
		for (Object o[] : hasMany) {
			String parentName = (String) o[2];
			if (parentName.equalsIgnoreCase(names[names.length - 2])) {
				ID itemId = (ID) o[0];
				return itemId;
			}
		}
		
		// 仅查找最后两级
		return null;
	}
	
	/**
	 * 获取指定项目的所处等级（注意从 0 开始）
	 * 
	 * @param itemId
	 * @return
	 */
	public static int getItemLevel(ID itemId) {
		int level = 0;
		ID parent = itemId;
		while (parent != null) {
			Object o[] = Application.createQueryNoFilter(
					"select parent from ClassificationData where itemId = ?")
					.setParameter(1, parent)
					.unique();
			if (o != null && o[0] != null) {
				level++;
				parent = (ID) o[0];
			} else {
				parent = null;
			}
		}
		return level;
	}
	
	/**
	 * 获取开放级别（注意从 0 开始）
	 * 
	 * @param field
	 * @return
	 */
	public static int getOpenLevel(Field field) {
		ID dataId = getUseClassification(field);
		if (dataId == null) {
			return 0;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select openLevel from Classification where dataId = ?")
				.setParameter(1, dataId)
				.unique();
		return o == null ? 0 : (Integer) o[0];
	}
	
	/**
	 * @param field
	 * @return
	 */
	private static ID getUseClassification(Field field) {
		String use = EasyMeta.valueOf(field).getFieldExtConfig().getString("classification");
		ID dataId = ID.isId(use) ? ID.valueOf(use) : null;
		if (dataId == null) {
			LOG.error("Field [ " + field + " ] unconfig classification");
		}
		return dataId;
	}
	
	/**
	 * TODO 清理缓存
	 * 
	 * @param dataOrItem
	 */
	public static void clearCache(ID dataOrItem) {
	}
}
