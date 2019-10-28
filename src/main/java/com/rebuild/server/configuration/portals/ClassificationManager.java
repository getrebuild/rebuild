/*
rebuild - Building your business-systems freely.
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

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/28
 */
public class ClassificationManager implements ConfigManager<ID> {

	private static final Log LOG = LogFactory.getLog(ClassificationManager.class);
	
	public static final ClassificationManager instance = new ClassificationManager();
	private ClassificationManager() { }

	private static final int BAD_CLASSIFICATION = -1;
	
	/**
	 * 获取名称
	 * 
	 * @param itemId
	 * @return
	 */
	public String getName(ID itemId) {
		String[] ns = getItemNames(itemId);
		return ns == null ? null : ns[0];
	}
	
	/**
	 * 获取全名称（包括父级，用 . 分割）
	 * 
	 * @param itemId
	 * @return
	 */
	public String getFullName(ID itemId) {
		String[] ns = getItemNames(itemId);
		return ns == null ? null : ns[1];
	}
	
	/**
	 * @param itemId
	 * @return [名称, 全名称]
	 */
	private String[] getItemNames(ID itemId) {
		final String ckey = "ClassificationNAME-" + itemId;
		String[] cached = (String[]) Application.getCommonCache().getx(ckey);
		if (cached != null) {
			return cached;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select name,fullName from ClassificationData where itemId = ?")
				.setParameter(1, itemId)
				.unique();
		if (o != null) {
			cached = new String[] { (String) o[0], (String) o[1] };
			Application.getCommonCache().putx(ckey, cached);
		}
		return cached;
	}
	
	/**
	 * 根据名称搜索对应的分类项 ID（后段匹配优先）
	 * 
	 * @param name
	 * @param field
	 * @return
	 */
	public ID findItemByName(String name, Field field) {
		ID dataId = getUseClassification(field, false);
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
	public int getOpenLevel(Field field) {
		ID dataId = getUseClassification(field, false);
		if (dataId == null) {
			return BAD_CLASSIFICATION;
		}
		
		String ckey = "ClassificationLEVEL-" + dataId;
		Integer cval = (Integer) Application.getCommonCache().getx(ckey);
		if (cval != null) {
			return cval;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select openLevel from Classification where dataId = ?")
				.setParameter(1, dataId)
				.unique();
		if (o == null) {
			return BAD_CLASSIFICATION;
		}
		
		cval = (Integer) o[0];
		Application.getCommonCache().putx(ckey, cval);
		return cval;
	}
	
	/**
	 * 获取指定字段所使用的分类
	 * 
	 * @param field
	 * @param verfiy
	 * @return
	 */
	public ID getUseClassification(Field field, boolean verfiy) {
		String use = EasyMeta.valueOf(field).getFieldExtConfig().getString("classification");
		ID dataId = ID.isId(use) ? ID.valueOf(use) : null;
		if (dataId == null) {
			LOG.error("Field [ " + field + " ] unconfig classification");
			return null;
		}
		
		if (verfiy && getOpenLevel(field) == BAD_CLASSIFICATION) {
			return null;
		}
		return dataId;
	}
	
	@Override
	public void clean(ID cacheKey) {
		if (cacheKey.getEntityCode() == EntityHelper.ClassificationData) {
			Application.getCommonCache().evict("ClassificationNAME-" + cacheKey);
		} else if (cacheKey.getEntityCode() == EntityHelper.Classification) {
			Application.getCommonCache().evict("ClassificationLEVEL-" + cacheKey);
		}
	}
}
