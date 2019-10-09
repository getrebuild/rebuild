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
import com.rebuild.server.configuration.ConfigEntry;

import cn.devezhao.persist4j.engine.ID;

/**
 * 基础布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public class BaseLayoutManager extends SharableManager<ID> {
	
	public static final BaseLayoutManager instance = new BaseLayoutManager();
	protected BaseLayoutManager() { }
	
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
	public ConfigEntry getLayoutOfForm(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_FORM);
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public ConfigEntry getLayoutOfDatalist(ID user, String belongEntity) {
		return getLayoutConfig(user, belongEntity, TYPE_DATALIST);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public ConfigEntry getLayoutOfNav(ID user) {
		return getLayoutConfig(user, null, TYPE_NAV);
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	public ConfigEntry getLayoutConfig(ID user, String belongEntity, String applyType) {
		ID configUsed = detectUseConfig(user, belongEntity, applyType);
		if (configUsed == null) {
			return null;
		}
		return getLayoutConfig(configUsed);
	}

	/**
	 * @param configId
	 * @return
	 */
	protected ConfigEntry getLayoutConfig(ID configId) {
		final String ckey = "BaseLayoutManager-" + configId;
		ConfigEntry entry = (ConfigEntry) Application.getCommonCache().getx(ckey);
		if (entry != null) {
			return entry.clone();
		}

		Object[] o = Application.createQueryNoFilter(
				"select configId,config,shareTo from LayoutConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		entry = new ConfigEntry()
				.set("id", o[0])
				.set("config", JSON.parse((String) o[1]))
				.set("shareTo", o[2]);
		Application.getCommonCache().putx(ckey, entry);
		return entry.clone();
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
	
	@Override
	public void clean(ID cacheKey) {
		Application.getCommonCache().evict("BaseLayoutManager-" + cacheKey);

		Object[] c = Application.createQueryNoFilter(
		        "select belongEntity,applyType from LayoutConfig where configId = ?")
                .setParameter(1, cacheKey)
                .unique();
		if (c != null) {
            String ck = String.format("%s-%s-%s", "LayoutConfig", "N".equals(c[0]) ? null : c[0], c[1]);
            Application.getCommonCache().evict(ck);
        }
	}
}
