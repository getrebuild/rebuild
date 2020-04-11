/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.common;

import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * 外部URL监测跳转
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BasePageControll {

	@RequestMapping(value="/commons/url-safe", method=RequestMethod.GET)
	public ModelAndView safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = getParameterNotNull(request, "url");
		if (!url.startsWith("http")) {
            url = "http://" + url;
        }

		if (isTrusted(url)) {
			response.sendRedirect(url);
			return null;
		}

		ModelAndView mv = createModelAndView("/commons/url-safe.jsp");
		mv.getModel().put("outerUrl", url);
		return mv;
	}

	private static final Set<String> TRUSTED_URLS = new HashSet<>();
    /**
     * 是否可信 URL
     *
     * @param url
     * @return
     */
	public static boolean isTrusted(String url) {
        url = url.split("\\?")[0];
        if (url.contains(SysConfiguration.getHomeUrl())) {
            return true;
        }

        // 首次
        if (TRUSTED_URLS.isEmpty()) {
            TRUSTED_URLS.add("getrebuild.com");

            try {
                File s = ResourceUtils.getFile("classpath:trusted-url.json");
                try (InputStream is = new FileInputStream(s)) {
                    JSONArray array = JSONArray.parseObject(is, null);
                    for (Object o : array) {
                        TRUSTED_URLS.add(o.toString());
                    }
                } catch (IOException e) {
                    LOG.error("Couldn't read file `trusted-url.json` in classpath.", e);
                }
            } catch (FileNotFoundException e) {
                LOG.error("Couldn't read file `trusted-url.json` in classpath.", e);
            }
        }

        String host = url;
        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException ignored) {
        }

        for (String trusted : TRUSTED_URLS) {
            if (host.equals(trusted) || host.contains(trusted)) {
                return true;
            }
        }
        return false;
	}
}
