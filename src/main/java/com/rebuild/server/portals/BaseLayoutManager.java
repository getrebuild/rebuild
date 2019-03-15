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

package com.rebuild.server.portals;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;

import cn.devezhao.persist4j.engine.ID;

/**
 * 基础布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public abstract class BaseLayoutManager extends SharableManager {
	
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 数据列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 导航
	public static final String TYPE_NAV = "NAV";
	
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public static Object[] getLayoutOfForm(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_FORM);
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public static Object[] getLayoutOfDatalist(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_DATALIST);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public static Object[] getLayoutOfNav(ID user) {
		return getLayoutConfig(user, null, TYPE_NAV);
	}

	/**
	 * 获取布局配置
	 * 
	 * @param user
	 * @param belongEntity
	 * @param applyType
	 * @return [ID, JSONConfig]
	 */
	private static Object[] getLayoutConfig(ID user, String belongEntity, String applyType) {
		ID configUsed = detectUseConfig(user, "LayoutConfig", belongEntity, applyType);
		if (configUsed == null) {
			return null;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select configId,config,shareTo from LayoutConfig where configId = ?")
				.setParameter(1, configUsed)
				.unique();
		o[1] = JSON.parse((String) o[1]);
		return o;
	}
}
