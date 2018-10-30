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
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserService;
import com.rebuild.server.bizz.privileges.Department;
import com.rebuild.server.bizz.privileges.User;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
public class UserControll extends BaseControll {
	
	@RequestMapping("/app/User/view/{id}")
	public ModelAndView pageView(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/user-view.jsp", "User", user);
		mv.getModel().put("id", record);
		return mv;
	}
	
	@RequestMapping("/admin/bizuser/users")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/user-list.jsp", "User", user);
		JSON config = DataListManager.getColumnLayout("User", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}
	
	@RequestMapping("/admin/bizuser/dept-tree")
	public void deptTreeGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Object[][] topDepts = Application.createQuery(
				"select deptId from Department where parentDept is null")
				.array();
		
		JSONArray firsts = new JSONArray();
		for (Object dept[] : topDepts) {
			Department first = Application.getUserStore().getDepartment((ID) dept[0]);
			firsts.add(recursiveDeptTree(first));
		}
		writeSuccess(response, firsts);
	}
	
	/**
	 * 部门结构
	 * 
	 * @param parent
	 */
	private JSONObject recursiveDeptTree(Department parent) {
		JSONObject parentJson = new JSONObject();
		parentJson.put("id", parent.getIdentity().toString());
		parentJson.put("name", parent.getName());
		JSONArray children = new JSONArray();
		for (BusinessUnit child : parent.getChildren()) {
			children.add(recursiveDeptTree((Department) child));
		}
		if (!children.isEmpty()) {
			parentJson.put("children", children);
		}
		return parentJson;
	}
	
	@RequestMapping("/admin/bizuser/check-has-member")
	public void checkHasMember(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		int hasMember = -1;
		if (id.getEntityCode() == EntityHelper.Role) {
			hasMember = Application.getUserStore().getRole(id).getMembers().size();
		} else if (id.getEntityCode() == EntityHelper.Department) {
			hasMember = Application.getUserStore().getDepartment(id).getMembers().size();
		}
		writeSuccess(response, hasMember);
	}
	
	@RequestMapping("/admin/bizuser/change-dept")
	public void changeDept(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getIdParameterNotNull(request, "user");
		ID deptNew = getIdParameterNotNull(request, "dept");
		
		User u = Application.getUserStore().getUser(user);
		if (u.getOwningDept() != null && u.getOwningDept().getIdentity().equals(deptNew)) {
			writeSuccess(response);
			return;
		}
		
		Application.getBean(UserService.class).txChangeDept(user, deptNew);
		writeSuccess(response);
	}
	
	@RequestMapping("/admin/bizuser/change-role")
	public void changeRole(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getIdParameterNotNull(request, "user");
		ID roleNew = getIdParameterNotNull(request, "role");
		
		User u = Application.getUserStore().getUser(user);
		if (u.getOwningRole() != null && u.getOwningRole().getIdentity().equals(roleNew)) {
			writeSuccess(response);
			return;
		}
		
		Record record = EntityHelper.forUpdate(user, getRequestUser(request));
		record.setID("roleId", roleNew);
		Application.getBean(UserService.class).update(record);
		writeSuccess(response);
	}
}