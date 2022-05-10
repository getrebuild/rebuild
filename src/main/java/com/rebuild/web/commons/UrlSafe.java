/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 外部 URL 监测跳转
 *
 * @author Zixin (RB)
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BaseController {

    @GetMapping("/commons/url-safe")
    public ModelAndView safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = getParameterNotNull(request, "url");
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = "http://" + url;
        }

        if (isTrusted(url)) {
            response.sendRedirect(url);
            return null;
        }

        return createModelAndView("/common/url-safe", Collections.singletonMap("outerUrl", url));
    }

    // --

    private static JSONArray TRUSTED_URLS;

    /**
     * 是否可信 URL
     *
     * @param url
     * @return
     */
    public static boolean isTrusted(String url) {
        url = url.split("\\?")[0];
        if (url.contains(RebuildConfiguration.getHomeUrl())) {
            return true;
        }

        // 首次
        if (TRUSTED_URLS == null) {
            String s = CommonsUtils.getStringOfRes("trusted-urls.json");
            TRUSTED_URLS = JSON.parseArray(StringUtils.defaultIfBlank(s, JSONUtils.EMPTY_ARRAY_STR));
            TRUSTED_URLS.add("getrebuild.com");
        }

        for (Object t : TRUSTED_URLS) {
            if (url.contains((String) t)) return true;
        }
        return false;
    }
}
