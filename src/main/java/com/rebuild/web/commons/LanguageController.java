/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.core.Application;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 多语言控制
 *
 * @author devezhao
 * @since 2019/11/29
 */
@Controller
@RequestMapping("/language/")
public class LanguageController extends BaseController {

    public static final String CK_LOCALE = "rb.locale";

    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String DIRECTIVE_NO_STORE = "no-store";

    // Support Etag
    // see org.springframework.web.filter.ShallowEtagHeaderFilter
    @GetMapping("bundle")
    public void getLanguageBundle(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(ServletUtils.CT_JS);

        final LanguageBundle bundle = AppUtils.getReuqestBundle(request);

        // whether the generated ETag should be weak
        // SPEC: length of W/ + " + 0 + 32bits md5 hash + "
        String responseETag = String.format("W/\"0%s\"", bundle.getBundleHash());
        response.setHeader(HEADER_ETAG, responseETag);

        // 无缓存
        String cacheControl = response.getHeader(HEADER_CACHE_CONTROL);
        if (cacheControl != null && cacheControl.contains(DIRECTIVE_NO_STORE)) {
            ServletUtils.write(response, "window._LANGBUNDLE = " + bundle.toJSON().toJSONString());
            return;
        }

        String requestETag = request.getHeader(HEADER_IF_NONE_MATCH);
        if (requestETag != null && ("*".equals(requestETag) || responseETag.equals(requestETag) ||
                responseETag.replaceFirst("^W/", "").equals(requestETag.replaceFirst("^W/", "")))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            ServletUtils.write(response, "window._LANGBUNDLE = " + bundle.toJSON().toJSONString());
        }
    }

    @GetMapping("select")
    public void selectLanguage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String locale = request.getParameter("locale");
        if (locale == null || (locale = Application.getLanguage().available(locale)) == null) {
            locale = Application.getLanguage().getDefaultBundle().getLocale();
        }

        if (Application.devMode()) Application.getLanguage().refresh();

        ServletUtils.setSessionAttribute(request, AppUtils.SK_LOCALE, locale);
        ServletUtils.addCookie(response, CK_LOCALE, locale);

        if (AppUtils.isHtmlRequest(request)) {
            String nexturl = request.getParameter("nexturl");
            if ("login".equals(nexturl)) {
                nexturl = AppUtils.getContextPath() + "/user/login?locale=" + getParameter(request, "locale");
            } else if ("install".equals(nexturl)) {
                nexturl = AppUtils.getContextPath() + "/setup/install?locale=" + getParameter(request, "locale");
            }

            nexturl = StringUtils.defaultIfBlank(nexturl, AppUtils.getContextPath());
            response.sendRedirect(CodecUtils.urlDecode(nexturl));
        } else {
            writeSuccess(response);
        }
    }
}
