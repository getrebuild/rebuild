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

package com.rebuild.web.admin.bizz;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * URL-Rewrite
 * 
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/app/")
public class BizzPage extends BaseEntityControll {

	@RequestMapping("User/view/{id}")
	public ModelAndView userView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/user-view.jsp", "User", getRequestUser(request));
		mv.getModel().put("id", record);
		return mv;
	}
	
	@RequestMapping("Department/view/{id}")
	public ModelAndView deptView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-view.jsp", "Department", getRequestUser(request));
		mv.getModel().put("id", record);
		return mv;
	}
	
	@RequestMapping("Role/view/{id}")
	public ModelAndView roleView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/role-view.jsp", "Role", getRequestUser(request));
		mv.getModel().put("id", record);
		return mv;
	}
}
