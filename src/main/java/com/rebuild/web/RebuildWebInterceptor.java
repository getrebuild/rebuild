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
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.CsrfToken;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.AppUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求拦截
 * - 检查授权
 * - 设置前端页面公用变量
 * - 设置请求用户、语言（线程量）
 *
 * @author Zhao Fangfang
 * @since 2.0
 */
@Slf4j
public class RebuildWebInterceptor implements AsyncHandlerInterceptor, InstallState {

    private static final ThreadLocal<RequestEntry> REQUEST_ENTRY = new NamedThreadLocal<>("RequestEntry");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        response.addHeader("X-RB-Server", ServerStatus.STARTUP_ONCE + "/" + Application.BUILD);

        if (Application.isWaitLoad()) {
            throw new DefinedException(600, "Please wait while REBUILD starting up ...");
        }

        UserContextHolder.setReqip(ServletUtils.getRemoteAddr(request));

        // Locale
        final String locale = detectLocale(request, response);
        UserContextHolder.setLocale(locale);

        final RequestEntry requestEntry = new RequestEntry(request, locale);
        REQUEST_ENTRY.set(requestEntry);

        // 页面变量
        // 请求页面时如果是非 HTML 请求头可能导致失败，因为页面中需要用到语言包变量
        if (requestEntry.isHtmlRequest()) {
            // Lang
            request.setAttribute(WebConstants.LOCALE, requestEntry.getLocale());
            request.setAttribute(WebConstants.$BUNDLE, Application.getLanguage().getBundle(requestEntry.getLocale()));

            request.setAttribute(WebConstants.USE_THEME,
                    !requestEntry.getRequestUri().contains("/admin/") && License.isCommercial());
        }

        final String requestUri = requestEntry.getRequestUri();

        // 服务暂不可用
        if (!Application.isReady()) {
            // 已安装
            if (checkInstalled()) {
                log.error("Server Unavailable : " + requestEntry);

                if (requestUri.endsWith("/error") || requestUri.contains("/error/")) {
                    return true;
                } else {
                    sendRedirect(response, "/error/server-status", null);
                    return false;
                }
            }
            // 未安装
            else if (!requestUri.contains("/setup/")) {
                sendRedirect(response, "/setup/install", null);
                return false;
            }
            else {
                return true;
            }
        }

        final ID requestUser = requestEntry.getRequestUser();

        // 用户验证
        if (requestUser != null) {

            // 管理员后台
            if (requestUri.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (requestEntry.isHtmlRequest()) {
                    sendRedirect(response, "/user/admin-verify", requestUri);
                } else {
                    ServletUtils.writeJson(response, RespBody.error(HttpStatus.FORBIDDEN.value()).toJSONString());
                }
                return false;
            }

            UserContextHolder.setUser(requestUser);

            // 页面变量（登录后）
            if (requestEntry.isHtmlRequest()) {
                // Last active
                Application.getSessionStore().storeLastActive(request);

                // User
                request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(requestUser));
                request.setAttribute(ZeroEntry.AllowCustomNav.name(),
                        Application.getPrivilegesManager().allow(requestUser, ZeroEntry.AllowCustomNav));

                // Side collapsed
                String sidebarCollapsed = ServletUtils.readCookie(request, "rb.sidebarCollapsed");
                String sideCollapsedClazz = "false".equals(sidebarCollapsed) ? "" : "rb-collapsible-sidebar-collapsed";
                // Aside
                if (!(requestEntry.getRequestUri().contains("/admin/") || requestEntry.getRequestUri().contains("/setup/"))) {
                    String asideCollapsed = ServletUtils.readCookie(request, "rb.asideCollapsed");
                    if (!"false".equals(asideCollapsed)) sideCollapsedClazz += " rb-aside-collapsed";
                }
                request.setAttribute("sideCollapsedClazz", sideCollapsedClazz);

            }

        } else if (!isIgnoreAuth(requestUri)) {
            // 外部表单特殊处理（媒体字段上传/预览）
            if (requestUri.contains("/filex/") && CsrfToken.verify(request, false)) {
                return true;
            }

            log.warn("Unauthorized access {}", RebuildWebConfigurer.getRequestUrls(request));

            if (requestEntry.isHtmlRequest()) {
                sendRedirect(response, "/user/login", requestUri);
            } else {
                ServletUtils.writeJson(response, RespBody.error(HttpStatus.FORBIDDEN.value()).toJSONString());
            }

            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Notings
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理用户
        UserContextHolder.clear();

        RequestEntry requestEntry = REQUEST_ENTRY.get();
        REQUEST_ENTRY.remove();

        // 打印处理时间
        long time = requestEntry == null ? 0 : (System.currentTimeMillis() - requestEntry.getRequestTime());
        if (time > 1000) {
            log.warn("Method handle time {} ms. Request URL(s) {}", time, RebuildWebConfigurer.getRequestUrls(request));
        }
    }

    /**
     * 语言探测
     *
     * @param request
     * @param response
     * @return
     */
    private String detectLocale(HttpServletRequest request, HttpServletResponse response) {
        // 0. Session
        String locale = (String) ServletUtils.getSessionAttribute(request, AppUtils.SK_LOCALE);

        String urlLocale = request.getParameter("locale");
        if (StringUtils.isNotBlank(urlLocale) && !urlLocale.equals(locale)) {
            urlLocale = Application.getLanguage().available(urlLocale);

            if (urlLocale != null) {
                locale = urlLocale;

                ServletUtils.setSessionAttribute(request, AppUtils.SK_LOCALE, locale);
                ServletUtils.addCookie(response, AppUtils.CK_LOCALE, locale,
                        CommonsCache.TS_DAY * 90, null, StringUtils.defaultIfBlank(AppUtils.getContextPath(), "/"));

                if (Application.devMode()) {
                    Application.getLanguage().refresh();
                }
            }
        }
        if (locale != null) return locale;

        // 1. Cookie
        locale = ServletUtils.readCookie(request, AppUtils.CK_LOCALE);
        if (locale == null) {
            // 2. User-Local
            locale = request.getLocale().toString();
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
                || requestUri.startsWith("/f/") || requestUri.startsWith("/s/")
                || requestUri.startsWith("/setup/")
                || requestUri.startsWith("/gw/")
                || requestUri.startsWith("/language/")
                || requestUri.startsWith("/filex/access/")
                || requestUri.startsWith("/filex/download/")
                || requestUri.startsWith("/filex/img/")
                || requestUri.startsWith("/commons/announcements")
                || requestUri.startsWith("/commons/url-safe")
                || requestUri.startsWith("/commons/barcode/render")
                || requestUri.startsWith("/commons/theme/")
                || requestUri.startsWith("/account/user-avatar/")
                || requestUri.startsWith("/rbmob/env");
    }

    /**
     * 请求参数封装
     */
    @Data
    private static class RequestEntry {
        final long requestTime;
        final ID requestUser;
        final String requestUri;
        final boolean htmlRequest;
        final String locale;

        RequestEntry(HttpServletRequest request, String locale) {
            this.requestTime = System.currentTimeMillis();

            this.requestUri = request.getRequestURI()
                    + (request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
            this.htmlRequest = !ServletUtils.isAjaxRequest(request)
                    && MimeTypeUtils.TEXT_HTML.equals(AppUtils.parseMimeType(request));

            this.locale = locale;
            this.requestUser = AppUtils.getRequestUser(request, true);
        }

        @Override
        public String toString() {
            return requestUri;
        }
    }
}
