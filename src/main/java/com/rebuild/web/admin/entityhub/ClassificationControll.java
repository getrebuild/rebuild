/*
rebuild - Building your system freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.entityhub;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.web.BasePageControll;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/27
 */
@Controller
@RequestMapping("/admin/entityhub/")
public class ClassificationControll extends BasePageControll {
	
	@RequestMapping("classifications")
	public ModelAndView pageIndex(HttpServletRequest request) throws IOException {
		Object[][] array = Application.createQuery(
				"select dataId,name,description from Classification order by name")
				.array();
		request.setAttribute("classifications", array);
		
		return createModelAndView("/admin/entityhub/classification/list.jsp");
	}
	
	@RequestMapping("classification/{id}")
	public ModelAndView pageData(@PathVariable String id,
			HttpServletRequest request) throws IOException {
		return createModelAndView("/admin/entityhub/classification/editor.jsp");
	}
}
