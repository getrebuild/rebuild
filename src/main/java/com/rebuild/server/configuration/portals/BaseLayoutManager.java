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

package com.rebuild.server.configuration.portals;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;

import cn.devezhao.persist4j.engine.ID;

/**
 * 基础布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public abstract class BaseLayoutManager<T> extends SharableManager<T> {
	
	// 导航
	public static final String TYPE_NAV = "NAV";
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 视图（相关项）
	public static final String TYPE_TAB = "TAB";
	// 视图（新建相关）
	public static final String TYPE_ADD = "ADD";
		
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public Object[] getLayoutOfForm(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_FORM);
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public Object[] getLayoutOfDatalist(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_DATALIST);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public Object[] getLayoutOfNav(ID user) {
		return getLayoutConfig(user, null, TYPE_NAV);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public Object[] getLayoutOfTab(ID user) {
		return getLayoutConfig(user, null, TYPE_TAB);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public Object[] getLayoutOfAdd(ID user) {
		return getLayoutConfig(user, null, TYPE_ADD);
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	public Object[] getLayoutConfig(ID user, String belongEntity, String applyType) {
		ID configUsed = detectUseConfig(user, belongEntity, applyType);
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
	
	/**
	 * @param user
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	public ID detectUseConfig(ID user, String belongEntity, String applyType) {
		return detectUseConfig(user, "LayoutConfig", belongEntity, applyType);
	}
}
