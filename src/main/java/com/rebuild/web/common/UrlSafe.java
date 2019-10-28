/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import java.util.HashSet;
import java.util.Set;

/**
 * 外部URL跳转
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BasePageControll {

	@RequestMapping(value="/commons/url-safe", method=RequestMethod.GET)
	public ModelAndView safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = getParameterNotNull(request, "url");
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

        if (TRUSTED_URLS.isEmpty()) {
            TRUSTED_URLS.add(".getrebuild.com");

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

        for (String trusted : TRUSTED_URLS) {
            if (url.contains(trusted)) {
                return true;
            }
        }
        return false;
	}
}
