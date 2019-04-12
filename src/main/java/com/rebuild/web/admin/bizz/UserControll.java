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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.portals.DataListManager;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseEntityControll;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class UserControll extends BaseEntityControll {
	
	@RequestMapping("users")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/user-list.jsp", "User", user);
		JSON config = DataListManager.getColumnLayout("User", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}
	
	@RequestMapping("check-user-status")
	public void checkUserStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		User checked = Application.getUserStore().getUser(id);
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("active", checked.isActive());
		ret.put("system", checked.getName().equals("system") || checked.getName().equals("admin"));
		
		ret.put("disabled", checked.isDisabled());
		if (checked.getOwningRole() != null) {
			ret.put("role", checked.getOwningRole().getIdentity());
		}
		if (checked.getOwningDept() != null) {
			ret.put("dept", checked.getOwningDept().getIdentity());
		}
		
		writeSuccess(response, ret);
	}
	
	@RequestMapping("change-dept")
	public void changeDept(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getIdParameterNotNull(request, "user");
		ID deptNew = getIdParameterNotNull(request, "dept");
		
		User u = Application.getUserStore().getUser(user);
		if (u.getOwningDept() != null && u.getOwningDept().getIdentity().equals(deptNew)) {
			writeSuccess(response);
			return;
		}
		
		Application.getBean(UserService.class).updateDepartment(user, deptNew);
		writeSuccess(response);
	}
	
	@RequestMapping("change-role")
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
	
	@RequestMapping("deleting-checks")
	public void deleteChecks(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		
		int hasMember = 0;
		int hasChild = 0;
		if (id.getEntityCode() == EntityHelper.Department) {
			Department dept = Application.getUserStore().getDepartment(id);
			hasMember = dept.getMembers().size();
			hasChild = dept.getChildren().size();
		} else if (id.getEntityCode() == EntityHelper.Role) {
			Role role = Application.getUserStore().getRole(id);
			hasMember = role.getMembers().size();
		}
		
		JSONObject ret = JSONUtils.toJSONObject(new String[] { "hasMember", "hasChild" },
				new Object[] { hasMember, hasChild });
		writeSuccess(response, ret);
	}
}