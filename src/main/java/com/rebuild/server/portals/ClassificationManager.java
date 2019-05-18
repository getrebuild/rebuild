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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 分类数据
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
		String[] ns = getItemNames(itemId);
		return ns == null ? null : ns[0];
	}
	
	/**
	 * 获取全名称（包括父级，用 . 分割）
	 * 
	 * @param itemId
	 * @return
	 */
	public static String getFullName(ID itemId) {
		String[] ns = getItemNames(itemId);
		return ns == null ? null : ns[1];
	}
	
	/**
	 * @param itemId
	 * @return [名称, 全名称]
	 */
	private static String[] getItemNames(ID itemId) {
		final String ckey = "NAME-" + itemId;
		String[] cval = (String[]) Application.getCommonCache().getx(ckey);
		if (cval != null) {
			return cval;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select name,fullName from ClassificationData where itemId = ?")
				.setParameter(1, itemId)
				.unique();
		if (o != null) {
			cval = new String[] { (String) o[0], (String) o[1] };
			Application.getCommonCache().putx(ckey, cval);
		}
		return cval;
	}
	
	/**
	 * 根据名称搜索对应的分类项 ID（后段匹配优先）
	 * 
	 * @param name
	 * @param field
	 * @return
	 */
	public static ID findItemByName(String name, Field field) {
		ID dataId = getUseClassification(field);
		if (dataId == null) {
			return null;
		}
		
		// 后匹配
		String ql = String.format(
				"select itemId from ClassificationData where dataId = '%s' and fullName like '%%%s'", dataId, name);
		Object[][] hasMany = Application.createQueryNoFilter(ql).array();
		if (hasMany.length == 0) {
			return null;
		} else if (hasMany.length == 1) {
			return (ID) hasMany[0][0];
		} else {
			// TODO 多个匹配
			return (ID) hasMany[0][0];
		}
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
		
		String ckey = "LEVEL-" + dataId;
		Integer cval = (Integer) Application.getCommonCache().getx(ckey);
		if (cval != null) {
			return cval;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select openLevel from Classification where dataId = ?")
				.setParameter(1, dataId)
				.unique();
		if (o != null) {
			cval = (Integer) o[0];
			Application.getCommonCache().putx(ckey, cval);
		}
		return cval == null ? 0 : cval;
	}
	
	/**
	 * 获取指定字段所使用的分类
	 * 
	 * @param field
	 * @return
	 */
	public static ID getUseClassification(Field field) {
		String use = EasyMeta.valueOf(field).getFieldExtConfig().getString("classification");
		ID dataId = ID.isId(use) ? ID.valueOf(use) : null;
		if (dataId == null) {
			LOG.error("Field [ " + field + " ] unconfig classification");
		}
		return dataId;
	}
	
	/**
	 *  清理缓存
	 * 
	 * @param dataOrItem
	 * @see #getName(ID)
	 * @see #getFullName(ID)
	 * @see #getOpenLevel(Field)
	 */
	public static void cleanCache(ID dataOrItem) {
		if (dataOrItem.getEntityCode() == EntityHelper.ClassificationData) {
			Application.getCommonCache().evict("NAME-" + dataOrItem);
		} else if (dataOrItem.getEntityCode() == EntityHelper.Classification) {
			Application.getCommonCache().evict("LEVEL-" + dataOrItem);
		}
	}
}
