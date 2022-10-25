/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.PageTokenVerify;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.RecordBuilder;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * 导航渲染
 *
 * @author devezhao
 * @since 2020/6/16
 */
@Slf4j
public class NavBuilder extends NavManager {

    public static final NavBuilder instance = new NavBuilder();

    private NavBuilder() {
    }

    // 导航项属性
    private static final String[] NAV_ITEM_PROPS = new String[] { "icon", "text", "type", "value" };

    // 默认导航
    private static final JSONArray NAVS_DEFAULT = JSONUtils.toJSONObjectArray(
            NAV_ITEM_PROPS,
            new Object[][] {
                    new Object[] { "chart-donut", "动态", "BUILTIN", NAV_FEEDS },
                    new Object[] { "shape", "项目", "BUILTIN", NAV_PROJECT },
                    new Object[] { "folder", "文件", "BUILTIN", NAV_FILEMRG }
            });

    // 新建项目
    private static final JSONObject NAV_PROJECT__ADD = JSONUtils.toJSONObject(
            NAV_ITEM_PROPS,
            new String[] { "plus", "添加项目", "BUILTIN", NAV_PROJECT + "--add" }
    );

    /**
     * 获取指定用户的导航菜单
     *
     * @param user
     * @return
     */
    public JSONArray getUserNav(ID user) {
        ConfigBean config = getLayoutOfNav(user);
        if (config == null) {
            JSONArray useDefault = replaceLang(NAVS_DEFAULT);
            ((JSONObject) useDefault.get(1)).put("sub", buildAvailableProjects(user));
            return useDefault;
        }

        // 过滤
        JSONArray navs = (JSONArray) config.getJSON("config");
        for (Iterator<Object> iter = navs.iterator(); iter.hasNext(); ) {
            JSONObject nav = (JSONObject) iter.next();
            JSONArray subNavs = nav.getJSONArray("sub");

            // 父级菜单
            if (subNavs != null && !subNavs.isEmpty()) {
                for (Iterator<Object> subIter = subNavs.iterator(); subIter.hasNext(); ) {
                    JSONObject subNav = (JSONObject) subIter.next();
                    if (isFilterNavItem(subNav, user)) {
                        subIter.remove();
                    }
                }

                // 无子级，移除主菜单
                if (subNavs.isEmpty()) iter.remove();

            } else if (isFilterNavItem(nav, user)) {
                iter.remove();
            } else if (NAV_PROJECT.equals(nav.getString("value"))) {
                nav.put("sub", buildAvailableProjects(user));
            }
        }

        PTOKEN_IFNEED.remove();
        return navs;
    }

    private static final ThreadLocal<String> PTOKEN_IFNEED = new ThreadLocal<>();

    /**
     * 是否需要过滤掉
     *
     * @param item
     * @param user
     * @return
     */
    private boolean isFilterNavItem(JSONObject item, ID user) {
        String type = item.getString("type");
        String value = item.getString("value");

        if ("ENTITY".equalsIgnoreCase(type)) {
            if (NAV_PARENT.equals(value)) {
                return true;
            } else if (NAV_FEEDS.equals(value) || NAV_FILEMRG.equals(value) || NAV_PROJECT.equals(value)) {
                return false;
            } else if (!MetadataHelper.containsEntity(value)) {
                log.warn("Unknown entity in nav : " + value);
                return true;
            }

            return !Application.getPrivilegesManager().allowRead(
                    user, MetadataHelper.getEntity(value).getEntityCode());

        } else if ("URL".equals(type)) {

            // 替换 RBTOKEN
            // https://getrebuild.com/docs/rbv/openapi/page-token-verify

            if (value.contains("$RBTOKEN$") || value.contains("%24RBTOKEN%24")) {
                String ptoken = PTOKEN_IFNEED.get();
                if (ptoken == null) {
                    ptoken = PageTokenVerify.generate(UserContextHolder.getUser());
                    PTOKEN_IFNEED.set(ptoken);
                }

                if (value.contains("$RBTOKEN$")) value = value.replace("$RBTOKEN$", ptoken);
                else value = value.replace("%24RBTOKEN%24", ptoken);

                item.put("value", value);
            }

            // URL 绑定实体权限
            // 如 https://www.baidu.com/::ENTITY_NAME

            String[] ss = value.split("::");
            if (ss.length != 2) return false;

            String bindEntity = ss[1];
            if (MetadataHelper.containsEntity(bindEntity)) {
                item.put("value", ss[0]);
                return !Application.getPrivilegesManager().allowRead(user,
                        MetadataHelper.getEntity(bindEntity).getEntityCode());
            }
        }

        return false;
    }

    /**
     * 动态获取项目菜单
     *
     * @param user
     * @return
     */
    private JSONArray buildAvailableProjects(ID user) {
        ConfigBean[] projects = ProjectManager.instance.getAvailable(user);
        // 排序 a-z
        Arrays.sort(projects, Comparator.comparing(o -> o.getString("projectName")));

        JSONArray navsOfProjects = new JSONArray();
        JSONArray navsOfProjectsArchived = new JSONArray();
        for (ConfigBean e : projects) {
            JSONObject item = JSONUtils.toJSONObject(
                    NAV_ITEM_PROPS,
                    new Object[] { e.getString("iconName"), e.getString("projectName"), NAV_PROJECT, e.getID("id") });
            if (e.getInteger("status") == ProjectManager.STATUS_ARCHIVED) {
                navsOfProjectsArchived.add(item);
            } else {
                navsOfProjects.add(item);
            }
        }

        // 归档的
        if (!navsOfProjectsArchived.isEmpty()) {
            navsOfProjects.add(JSONUtils.toJSONObject(
                    NAV_ITEM_PROPS,
                    new String[] { null, Language.L("已归档"), NAV_DIVIDER, "ARCHIVED" }));
            navsOfProjects.addAll(navsOfProjectsArchived);
        }

        // 管理员显示新建项目入口
        if (UserHelper.isAdmin(user)) {
            JSONObject add = NAV_PROJECT__ADD.clone();
            replaceLang(add);
            navsOfProjects.add(add);
        }
        return navsOfProjects;
    }

    /**
     * 首次安装添加菜单
     *
     * @param initEntity
     */
    public void addInitNavOnInstall(String[] initEntity) {
        JSONArray initNav = replaceLang(NAVS_DEFAULT);

        for (String e : initEntity) {
            EasyEntity entity = EasyMetaFactory.valueOf(e);

            JSONObject navItem = JSONUtils.toJSONObject(
                    NAV_ITEM_PROPS,
                    new String[] { entity.getIcon(), entity.getLabel(), "ENTITY", entity.getName() });
            initNav.add(navItem);
        }

        Record record = RecordBuilder.builder(EntityHelper.LayoutConfig)
                .add("belongEntity", "N")
                .add("shareTo", SHARE_ALL)
                .add("applyType", TYPE_NAV)
                .add("config", initNav.toJSONString())
                .build(UserService.SYSTEM_USER);

        UserContextHolder.setUser(UserService.SYSTEM_USER);
        try {
            Application.getService(EntityHelper.LayoutConfig).create(record);
        } finally {
            UserContextHolder.clear();
        }
    }

    // -- PORTAL RENDER

    /**
     * @param request
     * @param activeNav
     * @return
     */
    public static String renderNav(HttpServletRequest request, String activeNav) {
        if (activeNav == null) activeNav = "dashboard-home";

        JSONArray navs = NavBuilder.instance.getUserNav(AppUtils.getRequestUser(request));

        StringBuilder navsHtml = new StringBuilder();
        for (Object item : navs) {
            navsHtml.append(renderNavItem((JSONObject) item, activeNav)).append('\n');
        }
        return navsHtml.toString();
    }

    /**
     * 渲染导航菜單
     *
     * @param item
     * @param activeNav
     * @return
     */
    protected static String renderNavItem(JSONObject item, String activeNav) {
        final String navType = item.getString("type");
        final boolean isUrlType = "URL".equals(navType);
        String navName = item.getString("value");
        String navUrl = item.getString("value");

        String navEntity = null;

        boolean isOutUrl = isUrlType && navUrl.startsWith("http");
        if (isUrlType) {
            navName = "nav_url-" + navName.hashCode();

            if (isOutUrl) {
                navUrl = AppUtils.getContextPath("/commons/url-safe?url=" + CodecUtils.urlEncode(navUrl));
            } else {
                navUrl = AppUtils.getContextPath(navUrl);
            }

        } else if (NAV_FEEDS.equals(navName)) {
            navName = "nav_entity-FEEDS";
            navUrl = AppUtils.getContextPath("/feeds/home");

        } else if (NAV_FILEMRG.equals(navName)) {
            navName = "nav_entity-ATTACHMENT";
            navUrl = AppUtils.getContextPath("/files/home");

        } else if (NAV_PROJECT.equals(navName)) {
            navName = "nav_entity-PROJECT";
            navUrl = AppUtils.getContextPath("/project/search");

        } else if (NAV_PROJECT.equals(navType)) {
            navName = "nav_project-" + navName;
            navUrl = String.format("%s/project/%s/tasks", AppUtils.getContextPath(), navUrl);

        } else if (navName.startsWith(NAV_PROJECT)) {
            navName = "nav_project--add";
            navUrl = AppUtils.getContextPath("/admin/projects");

        } else {
            navEntity = navName;
            navName = "nav_entity-" + navName;
            navUrl = AppUtils.getContextPath("/app/" + navUrl + "/list");
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

        String navItemHtml;
        if (NAV_DIVIDER.equals(navType)) {
            navItemHtml = "<li class=\"divider\">" + navText;
        } else {
            String parentClass = " parent";
            if (BooleanUtils.toBoolean(item.getString("open"))) parentClass += " open";

            navItemHtml = String.format(
                    "<li class=\"%s\" data-entity=\"%s\"><a href=\"%s\" target=\"%s\"><i class=\"icon zmdi zmdi-%s\"></i><span>%s</span></a>",
                    navName + (subNavs == null ? StringUtils.EMPTY : parentClass),
                    navEntity == null ? StringUtils.EMPTY : navEntity,
                    subNavs == null ? navUrl : "###",
                    isOutUrl ? "_blank" : "_self",
                    navIcon,
                    navText);
        }
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
                if (activeNav.startsWith("nav_entity-") || activeNav.startsWith("nav_project-")) {
                    Element navParent = nav.parent();
                    if (navParent != null && navParent.hasClass("sub-menu-ul")) {
                        //noinspection ConstantConditions
                        navParent.parent().parent().parent().parent().addClass("open active");
                    }
                }
            }
            //noinspection ConstantConditions
            return navBody.selectFirst("li").outerHtml();
        }
        return navHtml.toString();
    }

    // TODO 目前仅处理了默认导航

    @SuppressWarnings("SameParameterValue")
    private static JSONArray replaceLang(JSONArray resource) {
        JSONArray clone = (JSONArray) resource.clone();
        for (Object o : clone) {
            replaceLang((JSONObject) o);
        }
        return clone;
    }

    private static void replaceLang(JSONObject item) {
        String text = item.getString("text");
        item.put("text", Language.L(text));
    }
}
