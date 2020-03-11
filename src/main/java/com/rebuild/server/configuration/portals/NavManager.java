/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 导航菜单
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
public class NavManager extends BaseLayoutManager {

	// 父菜单
	public static final String NAV_PARENT = "$PARENT$";
	// 动态
	public static final String NAV_FEEDS = "$FEEDS$";
	// 文件
	public static final String NAV_FILEMRG = "$FILEMRG$";

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

	/**
	 * 获取可用导航ID
	 *
	 * @param user
	 * @return
	 */
	public ID[] getUsesNavId(ID user) {
		Object[][] uses = getUsesConfig(user, null, TYPE_NAV);
		List<ID> array = new ArrayList<>();
		for (Object[] c : uses) {
			array.add((ID) c[0]);
		}
		return array.toArray(new ID[0]);
	}

	// ----

    /**
     * 默认导航
     */
    private static final JSONArray NAVS_DEFAULT = JSONUtils.toJSONObjectArray(
			new String[] { "icon", "text", "type", "value" },
			new Object[][] {
					new Object[] { "chart-donut", "动态", "ENTITY", NAV_FEEDS },
					new Object[] { "folder", "文件", "ENTITY", NAV_FILEMRG }
			});

	/**
	 * @param request
	 * @return
	 */
	public JSONArray getNavForPortal(HttpServletRequest request) {
		return getNavForPortal(AppUtils.getRequestUser(request));
	}

	/**
	 * @param user
	 * @return
	 */
	public JSONArray getNavForPortal(ID user) {
		ConfigEntry config = getLayoutOfNav(user);
		if (config == null) {
			return NAVS_DEFAULT;
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
			if (NAV_PARENT.equals(entity)) {
				return true;
			} else if (NAV_FEEDS.equals(entity) || NAV_FILEMRG.equals(entity)) {
				return false;
			} else if (!MetadataHelper.containsEntity(entity)) {
				LOG.warn("Unknow entity in nav : " + entity);
				return true;
			}

			Entity entityMeta = MetadataHelper.getEntity(entity);
            return !Application.getSecurityManager().allowRead(user, entityMeta.getEntityCode());
		}
		return false;
	}

	/**
	 * 渲染导航菜單
	 *
	 * @param item
	 * @param activeNav
	 * @return
	 */
	public String renderNavItem(JSONObject item, String activeNav) {
		final boolean isUrlType = "URL".equals(item.getString("type"));
		String navName = item.getString("value");
		String navUrl = item.getString("value");

		if (isUrlType) {
			navName = "nav_url-" + navName.hashCode();
			navUrl = ServerListener.getContextPath() + "/commons/url-safe?url=" + CodecUtils.urlEncode(navUrl);
		} else if (NAV_FEEDS.equals(navName)) {
			navName = "nav_entity-Feeds";
			navUrl = ServerListener.getContextPath() + "/feeds/home";
		} else if (NAV_FILEMRG.equals(navName)) {
			navName = "nav_entity-Attachment";
			navUrl = ServerListener.getContextPath() + "/files/home";
		} else {
			navName = "nav_entity-" + navName;
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
				.append(String.format("<li class=\"%s\"><a href=\"%s\" target=\"%s\"><i class=\"icon zmdi zmdi-%s\"></i><span>%s</span></a>",
						navName + (subNavs == null ? StringUtils.EMPTY : " parent"),
						subNavs == null ? navUrl : "###",
						isUrlType ? "_blank" : "_self",
						navIcon,
						navText));
		if (subNavs != null) {
			StringBuilder subHtml = new StringBuilder()
					.append("<ul class=\"sub-menu\"><li class=\"title\">")
					.append(navText)
					.append("</li><li class=\"nav-items\"><div class=\"content\"><ul class=\"sub-menu-ul\">");

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
