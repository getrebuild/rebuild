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
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

/**
 * 导航渲染
 *
 * @author devezhao
 * @since 2020/6/16
 */
public class NavBuilder extends NavManager {

    public static final NavBuilder instance = new NavBuilder();
    private NavBuilder() { }

    // 默认导航
    private static final JSONArray NAVS_DEFAULT = JSONUtils.toJSONObjectArray(
            new String[] { "icon", "text", "type", "value" },
            new Object[][] {
                    new Object[] { "chart-donut", "动态", "BUILTIN", NAV_FEEDS },
                    new Object[] { "shape", "项目", "BUILTIN", NAV_PROJECT },
                    new Object[] { "folder", "文件", "BUILTIN", NAV_FILEMRG }
            });

    /**
     * @param request
     * @return
     */
    public JSONArray getNavPortal(HttpServletRequest request) {
        return getNavPortal(AppUtils.getRequestUser(request));
    }

    /**
     * @param user
     * @return
     */
    public JSONArray getNavPortal(ID user) {
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
            } else if (NAV_PROJECT.equals(nav.getString("value"))) {

                JSON projects = getProjects(user);
                if (projects == null) {
                    iter.remove();
                } else {
                    nav.put("sub", projects);
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
    private boolean isFilterNav(JSONObject nav, ID user) {
        String type = nav.getString("type");
        if ("ENTITY".equalsIgnoreCase(type)) {
            String entity = nav.getString("value");
            if (NAV_PARENT.equals(entity)) {
                return true;
            } else if (NAV_FEEDS.equals(entity) || NAV_FILEMRG.equals(entity) || NAV_PROJECT.equals(entity)) {
                return false;
            } else if (!MetadataHelper.containsEntity(entity)) {
                LOG.warn("Unknow entity in nav : " + entity);
                return true;
            }

            Entity entityMeta = MetadataHelper.getEntity(entity);
            return !Application.getPrivilegesManager().allowRead(user, entityMeta.getEntityCode());
        }
        return false;
    }

    /**
     * 动态获取项目菜单
     *
     * @param user
     * @return
     */
    private JSONArray getProjects(ID user) {
        JSONArray navsOfProject = new JSONArray();

        ConfigEntry[] projects = ProjectManager.instance.getAvailable(user);
        if (projects.length == 0) {
            if (UserHelper.isAdmin(user)) {
                JSONObject p = JSONUtils.toJSONObject(
                        new String[] { "icon", "text", "type", "value" },
                        new String[] { "plus", "添加项目", "BUILTIN", NAV_PROJECT + "--add" }
                );
                navsOfProject.add(p);

            } else {
                return null;
            }
        }

        for (ConfigEntry e : projects) {
            navsOfProject.add(JSONUtils.toJSONObject(
                    new String[] { "type", "text", "icon", "value" },
                    new Object[] { NAV_PROJECT, e.getString("projectName"), e.getString("iconName"), e.getID("id") }));
        }
        return navsOfProject;
    }

    // --

    /**
     * 渲染导航菜單
     *
     * @param item
     * @param activeNav
     * @return
     */
    public static String renderNavItem(JSONObject item, String activeNav) {
        final String navType = item.getString("type");
        final boolean isUrlType = "URL".equals(navType);
        String navName = item.getString("value");
        String navUrl = item.getString("value");

        boolean isOutUrl = isUrlType && navUrl.startsWith("http");
        if (isUrlType) {
            navName = "nav_url-" + navName.hashCode();
            if (isOutUrl) {
                navUrl = ServerListener.getContextPath() + "/commons/url-safe?url=" + CodecUtils.urlEncode(navUrl);
            } else {
                navUrl = ServerListener.getContextPath() + navUrl;
            }

        } else if (NAV_FEEDS.equals(navName)) {
            navName = "nav_entity-FEEDS";
            navUrl = ServerListener.getContextPath() + "/feeds/home";

        } else if (NAV_FILEMRG.equals(navName)) {
            navName = "nav_entity-ATTACHMENT";
            navUrl = ServerListener.getContextPath() + "/files/home";

        } else if (NAV_PROJECT.equals(navName)) {
            navName = "nav_entity-PROJECT";
            navUrl = ServerListener.getContextPath() + "/project/search";

        } else if (NAV_PROJECT.equals(navType)) {
            navName = "nav_project-" + navName;
            navUrl = String.format("%s/project/%s/tasks", ServerListener.getContextPath(), navUrl);

        } else if (navName.startsWith(NAV_PROJECT)) {
            navName = "nav_entity-PROJECT--add";
            navUrl = ServerListener.getContextPath() + "/admin/projects";

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

        String navItemHtml = String.format(
                "<li class=\"%s\"><a href=\"%s\" target=\"%s\"><i class=\"icon zmdi zmdi-%s\"></i><span>%s</span></a>",
                navName + (subNavs == null ? StringUtils.EMPTY : " parent"),
                subNavs == null ? navUrl : "###",
                isOutUrl ? "_blank" : "_self",
                navIcon,
                navText);
        StringBuilder navHtml = new StringBuilder(navItemHtml);

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
