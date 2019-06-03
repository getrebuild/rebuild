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

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.configuration.ConfigEntry;
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
public class NavManager extends BaseLayoutManager {

	public static final NavManager instance = new NavManager();
	private NavManager() { }
	
	/**
	 * @param user
	 * @return
	 */
	public JSON getNav(ID user) {
		ConfigEntry config = getLayoutOfNav(user);
		if (config == null) {
			return null;
		}
		return config.toJSON();
	}
	
	/**
	 * @param request
	 * @return
	 */
	public JSONArray getNavForPortal(HttpServletRequest request) {
		final ID user = AppUtils.getRequestUser(request);
		ConfigEntry config = getLayoutOfNav(user);
		if (config == null) {
			return JSONUtils.EMPTY_ARRAY;
		}
		
		// 过滤
		JSONArray navs = (JSONArray) config.getJSON("config");
		for (Iterator<Object> iter = navs.iterator(); iter.hasNext(); ) {
			JSONObject nav = (JSONObject) iter.next();
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
			} else if (isFilterNav(nav, user)) {
				iter.remove();
				continue;
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
	private boolean isFilterNav(JSONObject nav, ID user) {
		String type = nav.getString("type");
		if ("ENTITY".equalsIgnoreCase(type)) {
			String entity = nav.getString("value");
			if ("$PARENT$".equals(entity)) {
				return true;
			} else if (!MetadataHelper.containsEntity(entity)) {
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
	
	// --
	
	/**
	 * @param item
	 * @param activeNav
	 * @param isTop
	 * @return
	 */
	public String renderNavItem(JSONObject item, String activeNav, boolean isTop) {
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
		if (isTop) {
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
		}
		navHtml = String.format(navHtml, navName, clazz, navUrl, isUrlType ? "_blank" : "_self", navIcon, navText);
		
		if (subHas) {
			String subHtml = "<ul class=\"sub-menu\"><li class=\"title\">%s</li><li class=\"nav-items\"><div class=\"content\"><ul>";
			subHtml = String.format(subHtml, navText);
			
			for (Object o : subNavs) {
				JSONObject subNav = (JSONObject) o;
				subHtml += renderNavItem(subNav, activeNav, false);
			}
			subHtml += "</ul></div></li></ul>";
			navHtml += subHtml;
		}
		
		navHtml += "</li>";
		return navHtml;
	}
}
