/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.commons.LanguageController;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求拦截
 * - 检查授权
 * - 设置前端页面变量
 * - 设置请求用户（线程量）
 *
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-24
 */
public class RebuildWebInterceptor extends HandlerInterceptorAdapter implements InstallState {

    private static final Logger LOG = LoggerFactory.getLogger(RebuildWebInterceptor.class);

    private static final ThreadLocal<Long> REQUEST_TIME = new NamedThreadLocal<>("Request time start");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.getSession(true);
        response.addHeader("X-RB-Server", ServerStatus.STARTUP_ONCE);

        REQUEST_TIME.set(System.currentTimeMillis());

        if (Application.isWaitLoads()) {
            throw new DefinedException(600, "Please wait while REBUILD starting up ...");
        }

        final String requestUri = request.getRequestURI()
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
        final boolean htmlRequest = AppUtils.isHtmlRequest(request);

        // Locale
        final String locale = detectLocale(request);
        UserContextHolder.setLocale(locale);

        if (htmlRequest) {
            request.setAttribute(WebConstants.LOCALE, locale);
            request.setAttribute(WebConstants.$BUNDLE, Application.getLanguage().getBundle(locale));

            // TODO CSRF
            request.setAttribute(WebConstants.CSRF_TOKEN, CodecUtils.randomCode(60));

            // Side collapsed
            String sidebarCollapsed = ServletUtils.readCookie(request, "rb.sidebarCollapsed");
            String sideCollapsedClazz = "false".equals(sidebarCollapsed) ? "" : "rb-collapsible-sidebar-collapsed";
            // Aside
            if (!(requestUri.contains("/admin/") || requestUri.contains("/setup/"))) {
                String asideCollapsed = ServletUtils.readCookie(request, "rb.asideCollapsed");
                if (!"false".equals(asideCollapsed)) sideCollapsedClazz += " rb-aside-collapsed";
            }
            request.setAttribute("sideCollapsedClazz", sideCollapsedClazz);
        }

        // 服务状态
        if (!Application.isReady()) {
            boolean gotError = requestUri.endsWith("/error") || requestUri.contains("/error/");

            if (checkInstalled()) {
                LOG.error("Server Unavailable : " + requestUri);
                if (gotError) {
                    return true;
                } else {
                    sendRedirect(response, "/error/server-status", null);
                    return false;
                }

            } else if (!requestUri.contains("/setup/")) {
                sendRedirect(response, "/setup/install", null);
                return false;

            } else {
                return true;
            }
        }

        // 用户验证

        ID requestUser = AppUtils.getRequestUser(request);
        if (requestUser == null) {
            requestUser = AppUtils.getRequestUserViaRbMobile(request, true);
        }

        if (requestUser != null) {
            // 管理后台访问
            if (requestUri.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (htmlRequest) {
                    sendRedirect(response, "/user/admin-verify", requestUri);
                } else {
                    ServletUtils.writeJson(response, RespBody.error(401).toString());
                }
                return false;
            }

            UserContextHolder.setUser(requestUser);

            if (htmlRequest) {
                // Last active
                Application.getSessionStore().storeLastActive(request);

                // 前端使用
                request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(requestUser));
                request.setAttribute("AllowCustomNav",
                        Application.getPrivilegesManager().allow(requestUser, ZeroEntry.AllowCustomNav));
            }

        } else if (!isIgnoreAuth(requestUri)) {
            LOG.warn("Unauthorized access {} from {} via {}",
                    requestUri, StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "-"), ServletUtils.getRemoteAddr(request));

            if (htmlRequest) {
                sendRedirect(response, "/user/login", requestUri);
            } else {
                ServletUtils.writeJson(response, RespBody.error(403).toString());
            }
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理用户
        UserContextHolder.clear();

        // 打印处理时间
        Long time = REQUEST_TIME.get();
        REQUEST_TIME.remove();

        time = System.currentTimeMillis() - time;
        if (time > 1000) {
            LOG.warn("Method handle time {} ms. Request URL {} [ {} ]",
                    time, ServletUtils.getFullRequestUrl(request), StringUtils.defaultIfBlank(ServletUtils.getReferer(request), "-"));
        }
    }

    /**
     * @param request
     * @return
     */
    private String detectLocale(HttpServletRequest request) {
        // 0. Session
        String locale = (String) ServletUtils.getSessionAttribute(request, AppUtils.SK_LOCALE);
        if (locale != null) return locale;

        // 1. Cookie
        locale = ServletUtils.readCookie(request, LanguageController.CK_LOCALE);
        if (locale == null) {
            // 2. User-Local
            locale = request.getLocale().getLanguage();
        }

        // 3. Default
        if ((locale = Application.getLanguage().available(locale)) == null) {
            locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }

        ServletUtils.setSessionAttribute(request, AppUtils.SK_LOCALE, locale);
        return locale;
    }

    /**
     * @param response
     * @param url
     * @param nexturl
     * @throws IOException
     */
    private void sendRedirect(HttpServletResponse response, String url, String nexturl) throws IOException {
        String fullUrl = AppUtils.getContextPath() + url;
        if (nexturl != null) fullUrl += "?nexturl=" + CodecUtils.urlEncode(nexturl);
        response.sendRedirect(fullUrl);
    }

    /**
     * 忽略认证
     *
     * @param requestUri
     * @return
     */
    private boolean isIgnoreAuth(String requestUri) {
        if (requestUri.contains("/user/") && !requestUri.contains("/user/admin")) {
            return true;
        }

        requestUri = requestUri.split("\\?")[0];
        requestUri = requestUri.replaceFirst(AppUtils.getContextPath(), "");

        return requestUri.length() < 3
                || requestUri.endsWith("/error") || requestUri.contains("/error/")
                || requestUri.startsWith("/t/") || requestUri.startsWith("/s/")
                || requestUri.startsWith("/setup/")
                || requestUri.startsWith("/gw/")
                || requestUri.startsWith("/language/")
                || requestUri.startsWith("/filex/access/")
                || requestUri.startsWith("/commons/announcements")
                || requestUri.startsWith("/commons/url-safe")
                || requestUri.startsWith("/commons/barcode/render");
    }
}
