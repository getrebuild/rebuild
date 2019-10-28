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

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.service.bizz.DepartmentService;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class DepartmentControll extends BaseEntityControll {

	@RequestMapping("departments")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/dept-list.jsp", "Department", user);
		JSON config = DataListManager.instance.getFieldsLayout("Department", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}
	
	@RequestMapping( value = "dept-delete", method = RequestMethod.POST)
	public void deptDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID dept = getIdParameterNotNull(request, "id");
		ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新部门
		
		Application.getBean(DepartmentService.class).deleteAndTransfer(dept, transfer);
		writeSuccess(response);
	}
	
	@RequestMapping("dept-tree")
	public void deptTreeGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Object[][] rootDepts = Application.createQuery(
				"select deptId from Department where parentDept is null")
				.array();
		
		JSONArray roots = new JSONArray();
		for (Object dept[] : rootDepts) {
			Department root = Application.getUserStore().getDepartment((ID) dept[0]);
			roots.add(recursiveDeptTree(root));
		}
		writeSuccess(response, roots);
	}
	
	/**
	 * 部门结构
	 * 
	 * @param parent
	 */
	private JSONObject recursiveDeptTree(Department parent) {
		JSONObject parentJson = new JSONObject();
		parentJson.put("id", parent.getIdentity());
		parentJson.put("name", parent.getName());
		parentJson.put("disabled", parent.isDisabled());
		JSONArray children = new JSONArray();
		for (BusinessUnit child : parent.getChildren()) {
			children.add(recursiveDeptTree((Department) child));
		}
		if (!children.isEmpty()) {
			parentJson.put("children", children);
		}
		return parentJson;
	}
}
