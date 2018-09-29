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

package com.rebuild.server.helper.manager;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;

/**
 * 布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public class LayoutManager {
	
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 视图
	public static final String TYPE_VIEW = "VIEW";
	// 数据列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 导航
	public static final String TYPE_NAVI = "NAVI";
	
	/**
	 * @param entity
	 * @param type
	 * @return
	 */
	public static Object[] getLayoutConfigRaw(String entity, String type) {
		Object[] config = Application.createNoFilterQuery(
				"select layoutId,config from LayoutConfig where type = ? and belongEntity = ?")
				.setParameter(1, type)
				.setParameter(2, entity)
				.unique();
		if (config == null) {
			return null;
		}
		config[1] = JSON.parse(config[1].toString());
		return config;
	}
}
