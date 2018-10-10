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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
public class DepartmentControll extends BaseControll {

	@RequestMapping("/admin/bizuser/departments")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-list.jsp", "Department");
		JSON cfg = DataListManager.getColumnLayout("Department");
		mv.getModel().put("DataListConfig", JSON.toJSONString(cfg));
		return mv;
	}
	
	@RequestMapping("/app/Department/view/{id}")
	public ModelAndView pageView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID recordId = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-view.jsp", "Department");
		mv.getModel().put("id", recordId);
		return mv;
	}
}
