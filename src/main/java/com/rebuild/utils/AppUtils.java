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
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.web.admin.AdminVerfiyController;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.sql.DataTruncation;

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

    /**
     * @return
     * @see BootApplication#getContextPath()
     */
    public static String getContextPath() {
        return BootApplication.getContextPath();
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
        return ServletUtils.getSessionAttribute(request, AdminVerfiyController.KEY_VERIFIED) != null;
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
                return Language.L("Error404");
            } else if (code != null && code == 403) {
                return Language.L("Error403");
            }

            exception = (Throwable) request.getAttribute(ServletUtils.ERROR_EXCEPTION);
        }

        // 已知异常
        if (exception != null) {
            Throwable known = ThrowableUtils.getRootCause(exception);
            if (known instanceof DataTruncation) {
                return Language.L("ErrorOutMaxInput");
            } else if (known instanceof AccessDeniedException) {
                return Language.L("Error403");
            }
        }

        if (exception == null) {
            return Language.L("Error500");
        } else {
            exception = ThrowableUtils.getRootCause(exception);
            String errorMsg = exception.getLocalizedMessage();
            if (StringUtils.isBlank(errorMsg)) errorMsg = Language.L("Error500");
            return errorMsg;
        }
    }

    /**
     * 是否移动端请求
     *
     * @param request
     * @return
     */
    public static boolean isRbMobile(HttpServletRequest request) {
        String UA = request.getHeader("X-Client");
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
}
