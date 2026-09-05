/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.web.admin.AdminVerfiyController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

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

    // v4.5 AccessKey 认证
    public static final String HF_AK = "Authorization";
    public static final String URL_AK = "ak";

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
        if (authToken != null) {
            return AuthTokenManager.verifyToken(authToken, false, refreshToken);
        }
        return getRequestUserViaAk(request, false);
    }

    /**
     * Header: Authorization: Bearer <AK> or URL: ?ak=<AK>
     * @param request
     * @param fromMcp
     * @return
     */
    public static ID getRequestUserViaAk(HttpServletRequest request, boolean fromMcp) {
        String auth = request.getHeader(HF_AK);
        String ak = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        // 兼容 <AK> 格式（去除首尾尖括号）
        if (ak != null && ak.startsWith("<") && ak.endsWith(">")) {
            ak = ak.substring(1, ak.length() - 1).trim();
        }
        if (fromMcp && ak != null) {
            return AuthTokenManager.verifyAccessKey(ak);
        }

        if (CommandArgs.getBoolean(CommandArgs._EnableAkAccess)) {
            if (ak == null) ak = request.getParameter(URL_AK);
            if (ak != null) return AuthTokenManager.verifyAccessKey(ak.trim());
        }
        return null;
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
     * @param user
     * @param wt
     * @return
     */
    public static String formatWatermarkText(ID user, String wt) {
        wt = wt == null ? RebuildConfiguration.get(ConfigurationItem.MarkWatermarkFormat) : wt;
        if (StringUtils.isBlank(wt)) return null;

        // 兼容中文变量
        wt = wt.replace("{用户}", "{USER}");
        wt = wt.replace("{姓名}", "{NAME}");
        wt = wt.replace("{邮箱}", "{EMAIL}");
        wt = wt.replace("{电话}", "{PHONE}");
        wt = wt.replace("{系统}", "{SYS}");
        wt = wt.replace("{日期}", "{DATE}");

        User u = user == null ? null : Application.getUserStore().getUser(user);
        wt = wt.replace("{USER}", u == null ? "" : ("***" + user.toLiteral().substring(7)));
        wt = wt.replace("{NAME}", u == null ? "" : u.getFullName());
        wt = wt.replace("{EMAIL}", u == null ? "" : StringUtils.defaultIfBlank(u.getEmail(), ""));
        wt = wt.replace("{PHONE}", u == null ? "" : StringUtils.defaultIfBlank(u.getWorkphone(), ""));
        wt = wt.replace("{SYS}", RebuildConfiguration.get(ConfigurationItem.AppName));
        wt = wt.replace("{DATE}", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));

        return wt;
    }

    /**
     * 水印内容
     *
     * @param user
     * @param wt 优先使用
     * @return
     */
    public static String getWatermarkText(ID user, String wt) {
        wt = formatWatermarkText(user, wt);
        if (wt == null) return null;

        String[] ss = wt.split("\\s+");
        return JSON.toJSONString(ss);
    }
}
