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

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

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
	public JSON getNavLayout(ID user) {
		ConfigEntry config = getLayoutOfNav(user);
		return config == null ? null : config.toJSON();
	}

	/**
	 * @param cfgid
	 * @return
	 */
	public JSON getNavLayoutById(ID cfgid) {
		ConfigEntry config = getLayoutById(cfgid);
		return config == null ? null : config.toJSON();
	}

	// ----

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
            return !Application.getSecurityManager().allowedR(user, entityMeta.getEntityCode());
		}
		return false;
	}

	/**
	 * 渲染導航菜單
	 *
	 * @param item
	 * @param activeNav
	 * @return
	 */
	public String renderNavItem(JSONObject item, String activeNav) {
		String navName = "nav_entity-" + item.getString("value");
		boolean isUrlType = "URL".equals(item.getString("type"));
		String navUrl = item.getString("value");
		if (isUrlType) {
			navName = "nav_url-" + navName.hashCode();
			navUrl = ServerListener.getContextPath() + "/commons/url-safe?url=" + CodecUtils.urlEncode(navUrl);
		} else {
			navUrl = ServerListener.getContextPath() + "/app/" + navUrl + "/list";
		}
		String navIcon = StringUtils.defaultIfBlank(item.getString("icon"), "texture");
		String navText = item.getString("text");
		
		JSONArray subNavs = null;
		if (activeNav != null) {
			subNavs = item.getJSONArray("sub");
			if (subNavs == null || subNavs.isEmpty()) {
				subNavs = null;
			}
		}

		StringBuilder navHtml = new StringBuilder()
				.append(String.format("<li class='%s'><a href='%s'%s><i class='icon zmdi zmdi-%s'></i><span>%s</span></a>",
						navName + (subNavs == null ? StringUtils.EMPTY : " parent"),
						subNavs == null ? navUrl : "#",
						isUrlType ? " target='_blank' rel='noopener noreferrer'" : StringUtils.EMPTY,
						navIcon, navText));
		if (subNavs != null) {
			StringBuilder subHtml = new StringBuilder()
					.append("<ul class='sub-menu'><li class='title'>")
					.append(navText)
					.append("</li><li class='nav-items'><div class='content'><ul class='sub-menu-ul'>");

			for (Object o : subNavs) {
				JSONObject subNav = (JSONObject) o;
				subHtml.append(renderNavItem(subNav, null));
			}
			subHtml.append("</ul></div></li></ul>");
			navHtml.append(subHtml);
		}
		navHtml.append("</li>");

		if (activeNav != null) {
			Document navBody = Jsoup.parseBodyFragment(navHtml.toString());
			for (Element nav : navBody.select("." + activeNav)) {
				nav.addClass("active");
				if (activeNav.startsWith("nav_entity-")) {
					Element navParent = nav.parent();
					if (navParent != null && navParent.hasClass("sub-menu-ul")) {
						navParent.parent().parent().parent().parent().addClass("open active");
					}
				}
			}
			return navBody.selectFirst("li").outerHtml();
		}
		return navHtml.toString();
	}
}
