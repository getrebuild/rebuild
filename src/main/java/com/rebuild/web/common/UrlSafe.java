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

import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BasePageControll {

	@RequestMapping(value="/commons/url-safe", method=RequestMethod.GET)
	public ModelAndView safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = getParameterNotNull(request, "url");
		if (isSafe(url)) {
			response.sendRedirect(url);
			return null;
		}

		ModelAndView mv = createModelAndView("/commons/url-safe.jsp");
		mv.getModel().put("outerUrl", url);
		return mv;
	}

	/**
	 * @param url
	 * @return
	 */
	private boolean isSafe(String url) throws IOException {
		String host = new URL(url).getHost();
		return host.endsWith("getrebuild.com")
				|| host.equalsIgnoreCase(SysConfiguration.get(ConfigurableItem.HomeURL));
	}
}
