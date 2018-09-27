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

package com.rebuild.server.service.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.engine.ID;

/**
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
		Object[] sets = getLayoutConfigRaw("", TYPE_NAVI);
		if (sets == null) {
			return null;
		}
		
		JSONObject json = new JSONObject();
		json.put("id", sets[0]);
		json.put("config", sets[1]);
		return json;
	}
	
	/**
	 * @return
	 */
	public static JSONArray getNavForPortal() {
		Object[] sets = getLayoutConfigRaw("", TYPE_NAVI);
		return sets == null ? JSON.parseArray("[]") : (JSONArray) sets[1];
	}
}
