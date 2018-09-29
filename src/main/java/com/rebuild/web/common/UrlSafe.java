/*
rebuild - Building your system freely.
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class UrlSafe extends BaseControll {

	@RequestMapping(value="/commons/url-safe", method=RequestMethod.GET)
	public void safeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String url = getParameterNotNull(request, "url");
		
		// TODO 检查 URL 安全
		
		response.sendRedirect(url);
	}
}
