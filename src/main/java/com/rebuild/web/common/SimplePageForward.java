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

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Startup;
import com.rebuild.web.PageControll;

/**
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
@Controller
public class SimplePageForward extends PageControll {

	@RequestMapping(value={ "/page/**/*", "/admin/page/**/*" }, method = RequestMethod.GET)
	public ModelAndView page(HttpServletRequest request) {
		String path = request.getRequestURI().toString();
		// remove `context path` and `/page/`
		path = path.substring(Startup.getContextPath().length());
		path = path.replace("/page/", "/");
		path = path + ".jsp";
		
		return createModelAndView(path);
	}
}