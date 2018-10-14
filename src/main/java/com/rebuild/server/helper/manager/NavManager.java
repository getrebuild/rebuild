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

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.engine.ID;

/**
 * 导航菜单
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
public class NavManager extends LayoutManager {

	/**
	 * @param user
	 * @return
	 */
	public static JSON getNav(ID user) {
		Object[] cfgs = getLayoutConfigRaw("N", TYPE_NAVI, user);
		if (cfgs == null) {
			return null;
		}
		JSONObject cfgsJson = JSONUtils.toJSONObject(new String[] { "id", "config" }, cfgs);
		return cfgsJson;
	}
	
	/**
	 * 页面用
	 * 
	 * @param request
	 * @return
	 */
	public static JSONArray getNavForPortal(HttpServletRequest request) {
		ID user = AppUtils.getRequestUser(request);
		Object[] cfgs = getLayoutConfigRaw("N", TYPE_NAVI, user);
		return cfgs == null ? JSON.parseArray("[]") : (JSONArray) cfgs[1];
	}
	
	/**
	 * @param cfgid
	 * @param toAll
	 * @param user
	 * @return
	 */
	public static ID detectConfigId(ID cfgid, boolean toAll, ID user) {
		return LayoutManager.detectConfigId(cfgid, toAll, "N", TYPE_NAVI, user);
	}
}
