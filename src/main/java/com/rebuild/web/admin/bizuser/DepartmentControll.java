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

package com.rebuild.web.admin.bizuser;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.DepartmentService;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
public class DepartmentControll extends BaseControll {

	@RequestMapping("/app/Department/view/{id}")
	public ModelAndView pageView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-view.jsp", "Department", user);
		mv.getModel().put("id", record);
		return mv;
	}
	
	@RequestMapping("/admin/bizuser/departments")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-list.jsp", "Department", user);
		JSON config = DataListManager.getColumnLayout("Department", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}
	
	@RequestMapping( value = "/admin/bizuser/dept-delete", method = RequestMethod.POST)
	public void deptDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID dept = getIdParameterNotNull(request, "id");
		ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新部门
		
		Application.getBean(DepartmentService.class).delete(dept, transfer);
		writeSuccess(response);
	}
}
