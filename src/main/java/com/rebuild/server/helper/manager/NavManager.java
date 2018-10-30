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

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.CodecUtils;
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
		Object[] cfg = getLayoutConfigRaw("N", TYPE_NAVI, user);
		if (cfg == null) {
			return null;
		}
		cfg[0] = cfg[0].toString();
		JSONObject cfgJson = JSONUtils.toJSONObject(new String[] { "id", "config" }, cfg);
		return cfgJson;
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
			if (isFilterNav(nav, user)) {
				iter.remove();
			} else {
				JSONArray subNavs = nav.getJSONArray("sub");
				if (subNavs != null && !subNavs.isEmpty()) {
					for (Iterator<Object> subIter = subNavs.iterator(); subIter.hasNext(); ) {
						JSONObject subNav = (JSONObject) subIter.next();
						if (isFilterNav(subNav, user)) {
							subIter.remove();
						}
					}
					
					// 无子级，移除主菜单
					if (subNavs.isEmpty()) {
						iter.remove();
					}
				}
			}
		}
		return navs;
	}
	
	/**
	 * 是否需要过滤掉
	 * 
	 * @param nav
	 * @param user
	 * @return
	 */
	private static boolean isFilterNav(JSONObject nav, ID user) {
		String type = nav.getString("type");
		if ("ENTITY".equalsIgnoreCase(type)) {
			String entity = nav.getString("value");
			if (!MetadataHelper.containsEntity(entity)) {
				LOG.warn("Unknow entity in nav : " + entity);
				return true;
			}
			
			Entity entityMeta = MetadataHelper.getEntity(entity);
			if (!Application.getSecurityManager().allowedR(user, entityMeta.getEntityCode())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param cfgid
	 * @param toAll
	 * @param user
	 * @return
	 * @see LayoutManager#detectConfigId(ID, boolean, String, String, ID)
	 */
	public static ID detectConfigId(ID cfgid, boolean toAll, ID user) {
		return LayoutManager.detectConfigId(cfgid, toAll, "N", TYPE_NAVI, user);
	}
	
	/**
	 * @param item
	 * @param activeNav
	 * @param isSub
	 * @return
	 */
	public static String renderNavItem(JSONObject item, String activeNav, boolean is1Level) {
		String navName = "nav_entity-" + item.getString("value");
		boolean isUrlType = "URL".equals(item.getString("type"));
		String navUrl = item.getString("value");
		if (!isUrlType) {
			navUrl = ServerListener.getContextPath() + "/app/" + navUrl + "/list";
		} else {
			navName = "nav_url-" + navName.hashCode();
			navUrl = ServerListener.getContextPath() + "/commons/url-safe?url=" + CodecUtils.urlEncode(navUrl);
		}
		String navIcon = StringUtils.defaultIfBlank(item.getString("icon"), "texture");
		String navText = item.getString("text");
		
		boolean subHas = false;
		JSONArray subNavs = null;
		if (is1Level) {
			subNavs = item.getJSONArray("sub");
			if (subNavs != null && !subNavs.isEmpty()) {
				subHas = true;
			}
		}
		
		String navHtml = "<li id=\"%s\" class=\"%s\"><a href=\"%s\" target=\"%s\"><i class=\"icon zmdi zmdi-%s\"></i><span>%s</span></a>";
		String clazz = navName.equals(activeNav) ? "active " : "";
		if (subHas) {
			clazz += "parent";
			isUrlType = false;
			navUrl = "javascript:;";
//			navUrl = "###" + navUrl;
		}
		navHtml = String.format(navHtml, navName, clazz, navUrl, isUrlType ? "_blank" : "_self", navIcon, navText);
		
		if (subHas) {
			String subHtml = "<ul class=\"sub-menu\"><li class=\"title\">%s</li><li class=\"nav-items\"><div class=\"rb-scroller\"><div class=\"content\"><ul>";
			subHtml = String.format(subHtml, navText);
			
			for (Object o : subNavs) {
				JSONObject subNav = (JSONObject) o;
				subHtml += renderNavItem(subNav, activeNav, false);
			}
			subHtml += "</ul></div></div></li></ul>";
			navHtml += subHtml;
		}
		
		navHtml += "</li>";
		return navHtml;
	}
}
