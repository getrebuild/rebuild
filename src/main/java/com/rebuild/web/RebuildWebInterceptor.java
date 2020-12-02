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
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.ModelAndView;
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
@Slf4j
public class RebuildWebInterceptor extends HandlerInterceptorAdapter implements InstallState {

    private static final ThreadLocal<Long> REQUEST_TIME = new NamedThreadLocal<>("Request time start");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
//        TODO request.getSession(true);
        response.addHeader("X-RB-Server", ServerStatus.STARTUP_ONCE);

        REQUEST_TIME.set(System.currentTimeMillis());

        if (Application.isWaitLoads()) {
            throw new DefinedException(600, "Please wait while REBUILD starting up ...");
        }

        final String requestUri = request.getRequestURI()
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
        final boolean isHtmlRequest = !ServletUtils.isAjaxRequest(request)
                && MimeTypeUtils.TEXT_HTML.equals(AppUtils.parseMimeType(request));

        // Locale
        final String locale = detectLocale(request, response);
        UserContextHolder.setLocale(locale);

        if (isHtmlRequest) {
            request.setAttribute(WebConstants.LOCALE, locale);
            request.setAttribute(WebConstants.$BUNDLE, Application.getLanguage().getBundle(locale));

            // TODO CSRF
            String csrfToken = CodecUtils.randomCode(60);
            request.setAttribute(WebConstants.CSRF_TOKEN, csrfToken);

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
                log.error("Server Unavailable : " + requestUri);
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

        final ID requestUser = AppUtils.getRequestUser(request, true);

        if (requestUser != null) {
            // 管理后台访问
            if (requestUri.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (isHtmlRequest) {
                    sendRedirect(response, "/user/admin-verify", requestUri);
                } else {
                    ServletUtils.writeJson(response, RespBody.error(HttpStatus.FORBIDDEN.value()).toJSONString());
                }
                return false;
            }

            UserContextHolder.setUser(requestUser);


            if (isHtmlRequest) {
                // Last active
                Application.getSessionStore().storeLastActive(request);

                // 前端使用
                request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(requestUser));
                request.setAttribute(ZeroEntry.AllowCustomNav.name(),
                        Application.getPrivilegesManager().allow(requestUser, ZeroEntry.AllowCustomNav));
            }

        } else if (!isIgnoreAuth(requestUri)) {
            log.warn("Unauthorized access {} via {}",
                    RebuildWebConfigurer.getRequestUrls(request), ServletUtils.getRemoteAddr(request));

            if (isHtmlRequest) {
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
        super.postHandle(request, response, handler, modelAndView);
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
