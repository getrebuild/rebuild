/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
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

    private static final int CODE_STARTING = 600;
    private static final int CODE_MAINTAIN = 602;
    private static final int CODE_UNSAFE_USE = 603;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        response.addHeader("X-RB-Server", ServerStatus.STARTUP_ONCE + "/" + Application.BUILD);

        if (Application.isWaitLoad()) {
            throw new DefinedException(CODE_STARTING, "Please wait while REBUILD starting up ...");
        }

        final String ipAddr = ServletUtils.getRemoteAddr(request);
        UserContextHolder.setReqip(ipAddr);

        // Locale
        final String locale = detectLocale(request, response);
        UserContextHolder.setLocale(locale);

        final RequestEntry requestEntry = new RequestEntry(request, locale);
        REQUEST_ENTRY.set(requestEntry);

        // Lang
        request.setAttribute(WebConstants.LOCALE, requestEntry.getLocale());
        request.setAttribute(WebConstants.$BUNDLE, Application.getLanguage().getBundle(requestEntry.getLocale()));

        final String requestUri = requestEntry.getRequestUri();

        // 服务暂不可用
        if (!Application.isReady()) {
            final boolean isError = requestUri.endsWith("/error") || requestUri.contains("/error/");

            // 已安装
            if (checkInstalled()) {
                log.error("Server Unavailable : " + requestEntry);

                if (isError) {
                    return true;
                } else {
                    sendRedirect(response, "/error/server-status", null);
                    return false;
                }
            }
            // 未安装
            else if (!(requestUri.contains("/setup/") || requestUri.contains("/commons/theme/") || isError)) {
                sendRedirect(response, "/setup/install", null);
                return false;
            } else {
                return true;
            }
        }

        final ID requestUser = requestEntry.getRequestUser();

        boolean skipCheckSafeUse;

        // 用户验证
        if (requestUser != null) {

            // 管理中心二次验证
            if (requestUri.contains("/admin/") && !AppUtils.isAdminVerified(request)) {
                if (isHtmlRequest(requestUri, request)) {
                    sendRedirect(response, "/user/admin-verify", requestEntry.getRequestUriWithQuery());
                } else {
                    response.sendError(HttpStatus.FORBIDDEN.value());
                }
                return false;
            }

            UserContextHolder.setUser(requestUser);

            // User
            request.setAttribute(WebConstants.$USER, Application.getUserStore().getUser(requestUser));

            boolean isSecurityEnhanced = RebuildConfiguration.getBool(ConfigurationItem.SecurityEnhanced);

            if (isHtmlRequest(requestUri, request)) {
                // Last active
                Application.getSessionStore().storeLastActive(request);

                // Nav collapsed
                String sidebarCollapsed = ServletUtils.readCookie(request, "rb.sidebarCollapsed");
                String sideCollapsedClazz = BooleanUtils.toBoolean(sidebarCollapsed) ? "rb-collapsible-sidebar-collapsed" : "";
                // Aside collapsed
                if (!(requestUri.contains("/admin/") || requestUri.contains("/setup/"))) {
                    String asideCollapsed = ServletUtils.readCookie(request, "rb.asideCollapsed");
                    if ("false".equals(asideCollapsed));
                    else sideCollapsedClazz += " rb-aside-collapsed";
                }
                request.setAttribute("sideCollapsedClazz", sideCollapsedClazz);

                request.setAttribute(ZeroEntry.AllowCustomNav.name(),
                        Application.getPrivilegesManager().allow(requestUser, ZeroEntry.AllowCustomNav));

                // v3.6-b5
                if (isSecurityEnhanced) {
                    CommonsLog.createLog(CommonsLog.TYPE_ACCESS, requestUser, null, requestUri);
                }
            }

            // 非增强安全超管可访问
            if (isSecurityEnhanced) skipCheckSafeUse = false;
            else skipCheckSafeUse = UserHelper.isSuperAdmin(requestUser);

        } else if (!isIgnoreAuth(requestUri)) {
            // 独立验证逻辑
            if (requestUri.contains("/filex/")) return true;

            log.warn("Unauthorized access {}", RebuildWebConfigurer.getRequestUrls(request));

            if (isHtmlRequest(requestUri, request)) {
                sendRedirect(response, "/user/login", requestEntry.getRequestUriWithQuery());
            } else {
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            }

            return false;
        } else {
            skipCheckSafeUse = true;
        }

        if (!skipCheckSafeUse) checkSafeUse(ipAddr, requestEntry.getRequestUri());

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // v3.6 H5 session 时间
        if (AppUtils.isRbMobile(request)) {
            request.getSession().setMaxInactiveInterval(60 * 5);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestEntry requestEntry = REQUEST_ENTRY.get();
        REQUEST_ENTRY.remove();

        // 打印处理时间
        long time = requestEntry == null ? 0 : (System.currentTimeMillis() - requestEntry.getRequestTime());
        if (time > 1500) {
            log.warn("Method handle time {} ms. Request URL(s) {}", time, RebuildWebConfigurer.getRequestUrls(request));
        }

        // 清理用户
        UserContextHolder.clear();
    }

    private String detectLocale(HttpServletRequest request, HttpServletResponse response) {
        String rbmobLocale = request.getHeader(AppUtils.HF_LOCALE);
        if (rbmobLocale != null) return rbmobLocale;

        // 0. Session
        String useLocale = (String) ServletUtils.getSessionAttribute(request, AppUtils.SK_LOCALE);

        String urlLocale = request.getParameter("locale");
        if (StringUtils.isNotBlank(urlLocale) && !urlLocale.equals(useLocale)) {
            urlLocale = Application.getLanguage().available(urlLocale);

            if (urlLocale != null) {
                useLocale = urlLocale;

                ServletUtils.setSessionAttribute(request, AppUtils.SK_LOCALE, useLocale);
                ServletUtils.addCookie(response, AppUtils.CK_LOCALE, useLocale,
                        CommonsCache.TS_DAY * 90, null, StringUtils.defaultIfBlank(AppUtils.getContextPath(), "/"));

                if (Application.devMode()) Application.getLanguage().refresh();
            }
        }
        if (useLocale != null) return useLocale;

        // 1. Cookie
        useLocale = ServletUtils.readCookie(request, AppUtils.CK_LOCALE);
        if (useLocale == null) {
            // 2. User-Local
            useLocale = request.getLocale().toString();
        }

        // 3. Default
        if ((useLocale = Application.getLanguage().available(useLocale)) == null) {
            useLocale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }

        ServletUtils.setSessionAttribute(request, AppUtils.SK_LOCALE, useLocale);
        return useLocale;
    }

    private boolean isIgnoreAuth(String requestUri) {
        if (requestUri.contains("..")) return false;
        if (requestUri.contains("/user/") && !requestUri.contains("/user/admin")) return true;

        requestUri = requestUri.replaceFirst(AppUtils.getContextPath(), "");

        return requestUri.length() < 3
                || requestUri.endsWith("/error") || requestUri.contains("/error/")
                || requestUri.endsWith("/logout")
                || requestUri.startsWith("/f/")
                || requestUri.startsWith("/s/")
                || requestUri.startsWith("/gw/")
                || requestUri.startsWith("/setup/")
                || requestUri.startsWith("/language/")
                || requestUri.startsWith("/filex/access/")
                || requestUri.startsWith("/filex/download/")
                || requestUri.startsWith("/filex/img/")
                || requestUri.startsWith("/commons/announcements")
                || requestUri.startsWith("/commons/url-safe")
                || requestUri.startsWith("/commons/barcode/render")
                || requestUri.startsWith("/commons/theme/")
                || requestUri.startsWith("/account/user-avatar/")
                || requestUri.startsWith("/rbmob/env")
                || requestUri.startsWith("/h5app-download")
                || requestUri.startsWith("/apiman/");
    }

    private boolean isHtmlRequest(String requestUri, HttpServletRequest request) {
        if (ServletUtils.isAjaxRequest(request)
                || requestUri.contains("/assets/")
                || requestUri.contains("/commons/frontjs/")
                || requestUri.contains("/commons/theme/")
                || requestUri.contains("/filex/download/")
                || requestUri.startsWith("/filex/img/")) {
            return false;
        }

        try {
            String accept = StringUtils.defaultIfBlank(
                    request.getHeader("Accept"), MimeTypeUtils.TEXT_HTML_VALUE).split("[,;]")[0];
            if (MimeTypeUtils.ALL_VALUE.equals(accept) || MimeTypeUtils.TEXT_HTML_VALUE.equals(accept)) return true;

            MimeType mimeType = MimeTypeUtils.parseMimeType(accept);
            return MimeTypeUtils.TEXT_HTML.equals(mimeType);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void sendRedirect(HttpServletResponse response, String url, String nexturl) throws IOException {
        String redirectUrl = AppUtils.getContextPath(url);
        if (nexturl != null) redirectUrl += "?nexturl=" + CodecUtils.urlEncode(nexturl);
        response.sendRedirect(redirectUrl);
    }

    private void checkSafeUse(String ipAddr, String requestUri) throws DefinedException {
        if (!License.isRbvAttached()) return;

        if ("localhost".equals(ipAddr) || "127.0.0.1".equals(ipAddr)) {
            log.debug("Allow localhost/127.0.0.1 use : {}", requestUri);
            return;
        }

        Object allowIp = CommonsUtils.invokeMethod(
                "com.rebuild.rbv.commons.SafeUses#checkIp", ipAddr);
        if (!(Boolean) allowIp) {
            throw new DefinedException(CODE_UNSAFE_USE, Language.L("你的 IP 地址不在允许范围内"));
        }

        Object allowTime = CommonsUtils.invokeMethod(
                "com.rebuild.rbv.commons.SafeUses#checkTime", CalendarUtils.now());
        if (!(Boolean) allowTime) {
            throw new DefinedException(CODE_UNSAFE_USE, Language.L("当前时间不允许使用"));
        }
    }

    @Data
    private static class RequestEntry {
        final long requestTime;
        final String requestUri;
        final String requestUriWithQuery;
        final ID requestUser;
        final String locale;

        RequestEntry(HttpServletRequest request, String locale) {
            this.requestTime = System.currentTimeMillis();
            this.requestUri = request.getRequestURI();
            this.requestUriWithQuery = this.requestUri + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            this.requestUser = AppUtils.getRequestUser(request, true);
            this.locale = locale;
        }

        @Override
        public String toString() {
            return requestUriWithQuery;
        }
    }
}
