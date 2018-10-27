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

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
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
		cfgs[0] = cfgs[0].toString();
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
		if (cfgs == null) {
			return JSON.parseArray("[]");
		}
	
		// 过滤
		JSONArray navs = (JSONArray) cfgs[1];
		for (Iterator<Object> iter = navs.iterator(); iter.hasNext(); ) {
			JSONObject nav = (JSONObject) iter.next();
			if ("ENTITY".equalsIgnoreCase(nav.getString("type"))) {
				String entity = nav.getString("value");
				if (!MetadataHelper.containsEntity(entity)) {
					LOG.warn("Unknow entity in nav : " + entity);
					iter.remove();
					continue;
				}
				
				Entity entityMeta = MetadataHelper.getEntity(entity);
				if (!Application.getSecurityManager().allowedR(user, entityMeta.getEntityCode())) {
					iter.remove();
					continue;
				}
			}
		}
		return navs;
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
