/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.web.admin.AdminVerfiyController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 封裝一些有用的工具方法
 *
 * @author Zixin (RB)
 * @since 05/19/2018
 */
@Slf4j
public class AppUtils {

    // Token 认证
    public static final String HF_AUTHTOKEN = "X-AuthToken";
    public static final String URL_AUTHTOKEN = "_authToken";

    // Csrf 认证
    public static final String HF_CSRFTOKEN = "X-CsrfToken";
    public static final String URL_CSRFTOKEN = "_csrfToken";

    // Once 认证
    public static final String HF_ONCETOKEN = "X-OnceToken";
    public static final String URL_ONCETOKEN = "_onceToken";

    // 语言
    public static final String SK_LOCALE = WebUtils.KEY_PREFIX + ".LOCALE";
    public static final String CK_LOCALE = "rb.locale";

    // RbMob
    public static final String HF_CLIENT = "X-Client";
    public static final String HF_LOCALE = "X-ClientLocale";

    public static final String UTF8 = "utf-8";

    /**
     * 获取相对地址
     *
     * @return
     * @see BootApplication#getContextPath()
     * @see RebuildConfiguration#getHomeUrl()
     */
    public static String getContextPath() {
        return BootApplication.getContextPath();
    }

    /**
     * 获取相对地址
     *
     * @return
     * @see RebuildConfiguration#getHomeUrl(String)
     */
    public static String getContextPath(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        return BootApplication.getContextPath() + path;
    }

    /**
     * 获取请求用户
     *
     * @param request
     * @return null or UserID
     */
    public static ID getRequestUser(HttpServletRequest request) {
        return getRequestUser(request, false);
    }

    /**
     * 获取请求用户
     *
     * @param request
     * @return null or UserID
     * @see #getRequestUserViaToken(HttpServletRequest, boolean)
     */
    public static ID getRequestUser(HttpServletRequest request, boolean refreshToken) {
        Object user = null;
        try {
            user = request.getSession().getAttribute(WebUtils.CURRENT_USER);
        } catch (Exception resHasBeenCommitted) {
            log.debug("resHasBeenCommitted", resHasBeenCommitted);
        }

        if (user == null) user = getRequestUserViaToken(request, refreshToken);
        return user == null ? null : (ID) user;
    }

    /**
     * 从 Header[X-AuthToken] 中获取请求用户
     *
     * @param request
     * @param refreshToken 是否需要刷新 Token 有效期
     * @return null or UserID
     */
    protected static ID getRequestUserViaToken(HttpServletRequest request, boolean refreshToken) {
        String authToken = request.getHeader(HF_AUTHTOKEN);
        return authToken == null
                ? null : AuthTokenManager.verifyToken(authToken, false, refreshToken);
    }

    /**
     * @param request
     * @return
     */
    public static LanguageBundle getReuqestBundle(HttpServletRequest request) {
        return Application.getLanguage().getBundle(getReuqestLocale(request));
    }

    /**
     * @param request
     * @return
     */
    public static String getReuqestLocale(HttpServletRequest request) {
        // in URL
        String locale = request.getParameter("locale");
        // in Session
        if (locale == null) locale = (String) ServletUtils.getSessionAttribute(request, SK_LOCALE);
        // in Header
        if (locale == null) locale = request.getHeader(HF_LOCALE);
        // in System
        if (StringUtils.isBlank(locale)) locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        return locale;
    }

    /**
     * @param request
     * @return
     */
    public static boolean isAdminVerified(HttpServletRequest request) {
        return ServletUtils.getSessionAttribute(request, AdminVerfiyController.KEY_VERIFIED) != null;
    }

    /**
     * 是否移动端请求
     *
     * @param request
     * @return
     */
    public static boolean isRbMobile(HttpServletRequest request) {
        String UA = request.getHeader(HF_CLIENT);
        return UA != null && UA.startsWith("RB/Mobile-");
    }

    /**
     * 是否 IE11（加载 polyfill）
     *
     * @param request
     * @return
     */
    public static boolean isIE11(HttpServletRequest request) {
        // eg: Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko
        String ua = request.getHeader("user-agent");
        return ua != null && ua.contains("Trident/") && ua.contains("rv:11.");
    }

    /**
     * 是否移动端
     *
     * @param request
     * @return
     */
    public static boolean isMobile(HttpServletRequest request) {
        String ua = request.getHeader("user-agent");
        return ua != null && (ua.contains("Mobile") || ua.contains("iPhone") || ua.contains("Android"));
    }

    /**
     * 水印内容
     *
     * @param user
     * @return
     */
    public static String getWatermarkText(ID user) {
        String wt = RebuildConfiguration.get(ConfigurationItem.MarkWatermarkFormat);
        if (StringUtils.isBlank(wt)) return null;

        // 兼容中文变量
        wt = wt.replace("{用户}", "{USER}");
        wt = wt.replace("{姓名}", "{NAME}");
        wt = wt.replace("{邮箱}", "{EMAIL}");
        wt = wt.replace("{电话}", "{PHONE}");
        wt = wt.replace("{系统}", "{SYS}");

        List<String> t = new ArrayList<>();
        User u = user == null ? null : Application.getUserStore().getUser(user);
        for (String item : wt.split(" ")) {
            // 用户ID
            if (item.contains("{USER}")) {
                item = item.replace("{USER}", u == null ? "" : ("***" + user.toLiteral().substring(7)));
            }
            // 姓名
            if (item.contains("{NAME}")) {
                item = item.replace("{NAME}", u == null ? "" : u.getFullName());
            }
            // 邮箱
            if (item.contains("{EMAIL}")) {
                item = item.replace("{EMAIL}", u == null ? "" : StringUtils.defaultIfBlank(u.getEmail(), ""));
            }
            // 电话
            if (item.contains("{PHONE}")) {
                item = item.replace("{PHONE}", u == null ? "" : StringUtils.defaultIfBlank(u.getWorkphone(), ""));
            }
            // 系统名称
            if (item.contains("{SYS}")) {
                item = item.replace("{SYS}", RebuildConfiguration.get(ConfigurationItem.AppName));
            }
            t.add(item);
        }
        return JSON.toJSONString(t);
    }
}
