/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.web.admin.AdminVerfiyController;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 封裝一些有用的工具方法
 *
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {

    // Token 认证
    public static final String HF_AUTHTOKEN = "X-AuthToken";
    public static final String URL_AUTHTOKEN = "_authToken";

    // 语言
    public static final String SK_LOCALE = WebUtils.KEY_PREFIX + ".LOCALE";
    public static final String CK_LOCALE = "rb.locale";

    // RbMob
    public static final String HF_CLIENT = "X-Client";
    public static final String HF_LOCALE = "X-ClientLocale";

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
     * 获取当前 Session 请求用户
     *
     * @param request
     * @return null or UserID
     */
    public static ID getRequestUser(HttpServletRequest request) {
        return getRequestUser(request, false);
    }

    /**
     * 获取当前 Session 请求用户
     *
     * @param request
     * @return null or UserID
     * @see #getRequestUserViaToken(HttpServletRequest, boolean)
     */
    public static ID getRequestUser(HttpServletRequest request, boolean refreshToken) {
        Object user = request.getSession().getAttribute(WebUtils.CURRENT_USER);
        if (user == null) {
            user = getRequestUserViaToken(request, refreshToken);
        }
        return user == null ? null : (ID) user;
    }

    /**
     * 从 Header[X-AuthToken] 中获取请求用户
     *
     * @param request
     * @param refreshToken 是否需要刷新 Token 有效期
     * @return null or UserID
     */
    public static ID getRequestUserViaToken(HttpServletRequest request, boolean refreshToken) {
        String authToken = request.getHeader(HF_AUTHTOKEN);
        ID user = AuthTokenManager.verifyToken(authToken, false);
        if (user != null && refreshToken) {
            AuthTokenManager.refreshToken(authToken, AuthTokenManager.TOKEN_EXPIRES);
        }
        return user;
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
     * 请求类型
     *
     * @param request
     * @return
     * @see MimeTypeUtils#parseMimeType(String)
     */
    public static MimeType parseMimeType(HttpServletRequest request) {
        try {
            String acceptType = request.getHeader("Accept");
            if (acceptType == null || "*/*".equals(acceptType)) acceptType = request.getContentType();

            // Via Spider?
            if (StringUtils.isBlank(acceptType)) return MimeTypeUtils.TEXT_HTML;

            acceptType = acceptType.split("[,;]")[0];
            // Accpet ALL?
            if ("*/*".equals(acceptType)) return MimeTypeUtils.TEXT_HTML;

            return MimeTypeUtils.parseMimeType(acceptType);

        } catch (Exception ignore) {
        }
        return null;
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
        UserAgent ua = UserAgentUtil.parse(request.getHeader("user-agent"));
        return ua != null && ua.isMobile();
    }
}
