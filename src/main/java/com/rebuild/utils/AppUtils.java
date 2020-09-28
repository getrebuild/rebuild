/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.web.admin.AdminVerfiyController;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.sql.DataTruncation;

/**
 * 封裝一些有用的工具方法
 *
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {

    /**
     * 移动端 UA 前缀
     */
    public static final String MOILE_UA_PREFIX = "RB/MOBILE-";

    /**
     * 移动端 Token Header
     */
    public static final String MOBILE_HF_AUTHTOKEN = "X-AuthToken";

    /**
     * 语言
     */
    public static final String SK_LOCALE = WebUtils.KEY_PREFIX + ".LOCALE";

    /**
     * @return
     * @see BootApplication#getContextPath()
     */
    public static String getContextPath() {
        return BootApplication.getContextPath();
    }

    /**
     * 获取当前请求用户
     *
     * @param request
     * @return null or UserID
     * @see #getRequestUserViaRbMobile(HttpServletRequest, boolean)
     */
    public static ID getRequestUser(HttpServletRequest request) {
        Object user = request.getSession(true).getAttribute(WebUtils.CURRENT_USER);
        return user == null ? null : (ID) user;
    }

    /**
     * 获取 APP 请求用户
     *
     * @param request
     * @param refreshToken 是否需要刷新 Token 有效期
     * @return
     * @see #isRbMobile(HttpServletRequest)
     */
    public static ID getRequestUserViaRbMobile(HttpServletRequest request, boolean refreshToken) {
        if (isRbMobile(request)) {
            String xAuthToken = request.getHeader(MOBILE_HF_AUTHTOKEN);
            ID user = AuthTokenManager.verifyToken(xAuthToken, false);
            if (user != null && refreshToken) {
                AuthTokenManager.refreshToken(xAuthToken, AuthTokenManager.TOKEN_EXPIRES);
            }
            return user;
        }
        return null;
    }

    /**
     * @param request
     * @return
     */
    public static User getRequestUserBean(HttpServletRequest request) {
        ID user = getRequestUser(request);
        if (user == null) user = getRequestUserViaRbMobile(request, false);
        if (user == null) return null;
        return Application.getUserStore().getUser(user);
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
        String locale = (String) ServletUtils.getSessionAttribute(request, SK_LOCALE);
        if (locale == null) {
            locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }
        return locale;
    }

    /**
     * @param request
     * @return
     */
    public static boolean isAdminVerified(HttpServletRequest request) {
        Object verified = ServletUtils.getSessionAttribute(request, AdminVerfiyController.KEY_VERIFIED);
        return verified != null;
    }

    /**
     * 获取后台抛出的错误消息
     *
     * @param request
     * @param exception
     * @return
     */
    public static String getErrorMessage(HttpServletRequest request, Throwable exception) {
        if (exception == null && request != null) {
            String errorMsg = (String) request.getAttribute(ServletUtils.ERROR_MESSAGE);
            if (StringUtils.isNotBlank(errorMsg)) {
                return errorMsg;
            }

            Integer code = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
            if (code != null && code == 404) {
                return Language.getLang("Error404");
            } else if (code != null && code == 403) {
                return Language.getLang("Error403");
            }

            exception = (Throwable) request.getAttribute(ServletUtils.ERROR_EXCEPTION);
        }

        // 已知异常
        if (exception != null) {
            Throwable known = ThrowableUtils.getRootCause(exception);
            if (known instanceof DataTruncation) {
                return Language.getLang("ErrorOutMaxInput");
            } else if (known instanceof AccessDeniedException) {
                return Language.getLang("Error403");
            }
        }

        if (exception == null) {
            return Language.getLang("Error500");
        } else {
            exception = ThrowableUtils.getRootCause(exception);
            String errorMsg = exception.getLocalizedMessage();
            if (StringUtils.isBlank(errorMsg)) errorMsg = Language.getLang("Error500");

            if (Application.devMode() && !(exception instanceof DataSpecificationException)) {
                errorMsg += " (" + exception.getClass().getSimpleName() + ")";
            }
            return errorMsg;
        }
    }

    /**
     * 是否 APP
     *
     * @param request
     * @return
     */
    public static boolean isRbMobile(HttpServletRequest request) {
        String UA = request.getHeader("user-agent");
        return UA != null && UA.toUpperCase().startsWith(MOILE_UA_PREFIX);
    }

    /**
     * 是否 HTML 请求
     *
     * @param request
     * @return
     */
    public static boolean isHtmlRequest(HttpServletRequest request) {
        if (ServletUtils.isAjaxRequest(request)) return false;

        MediaType mediaType = null;
        try {
            String contentType = request.getContentType();
            if (contentType == null) {
                contentType = request.getHeader("Accept").split(",")[0];
            }
            mediaType = MediaType.valueOf(contentType);
        } catch (Exception ignore) {
        }
        return MediaType.TEXT_HTML.equals(mediaType) || MediaType.APPLICATION_XHTML_XML.equals(mediaType);
    }

    /**
     * @param response
     * @param name
     * @param value
     * @see ServletUtils#addCookie(HttpServletResponse, String, String, int, String, String)
     * @see ServletUtils#readCookie(HttpServletRequest, String)
     */
    public static void addCookie(HttpServletResponse response, String name, String value) {
        ServletUtils.addCookie(response, name, value, 60 * 60 * 24 * 90,
                null, StringUtils.defaultIfBlank(getContextPath(), "/"));
    }
}
