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

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class UserControll extends BaseEntityControll {
	
	private static final String MSG_ENABLED = "<p>%s 你的账户已激活！现在你可以登陆并使用系统。</p><div style='margin:10px 0'>登录地址 <a href='%s'>%s</a></div><p>首次登陆，建议你立即修改密码！如有任何登陆或使用问题，请与系统管理员联系。</p>";
	
	@RequestMapping("users")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/user-list.jsp", "User", user);
		JSON config = DataListManager.instance.getFieldsLayout("User", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}
	
	@RequestMapping("check-user-status")
	public void checkUserStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		User checkedUser = Application.getUserStore().getUser(id);
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("active", checkedUser.isActive());
		ret.put("system", checkedUser.getName().equals("system") || checkedUser.getName().equals("admin"));
		
		ret.put("disabled", checkedUser.isDisabled());
		if (checkedUser.getOwningRole() != null) {
			ret.put("role", checkedUser.getOwningRole().getIdentity());
			ret.put("roleDisabled", checkedUser.getOwningRole().isDisabled());
		}
		if (checkedUser.getOwningDept() != null) {
			ret.put("dept", checkedUser.getOwningDept().getIdentity());
			ret.put("deptDisabled", checkedUser.getOwningDept().isDisabled());
		}
		
		writeSuccess(response, ret);
	}
	
	@RequestMapping("enable-user")
	public void enableUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
		
		ID user = ID.valueOf(data.getString("user"));
		User u = Application.getUserStore().getUser(user);
		final boolean isDisabled = u.isDisabled();
		
		ID deptNew = null;
		ID roleNew = null;
		if (data.containsKey("dept")) {
			deptNew = ID.valueOf(data.getString("dept"));
			if (u.getOwningDept() != null && u.getOwningDept().getIdentity().equals(deptNew)) {
				deptNew = null;
			}
		}
		if (data.containsKey("role")) {
			roleNew = ID.valueOf(data.getString("role"));
			if (u.getOwningRole() != null && u.getOwningRole().getIdentity().equals(roleNew)) {
				roleNew = null;
			}
		}
		
		Boolean enableNew = null;
		if (data.containsKey("enable")) {
			enableNew = data.getBoolean("enable");
		}
		
		Application.getBean(UserService.class).updateEnableUser(user, deptNew, roleNew, enableNew);
	
		// 是否发送激活通知
		u = Application.getUserStore().getUser(user);
		if (isDisabled && u.isActive() && SMSender.availableMail()) {
			Object did = Application.createQuery(
					"select logId from LoginLog where user = ?")
					.setParameter(1, u.getId())
					.unique();
			if (did == null) {
				String homeUrl = SysConfiguration.getHomeUrl();
				String content = String.format(MSG_ENABLED, u.getFullName(), homeUrl, homeUrl);
				SMSender.sendMailAsync(u.getEmail(), "你的账户已激活", content);
			}
		}
		
		writeSuccess(response);
	}
	
	@RequestMapping("delete-checks")
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
		} else if (id.getEntityCode() == EntityHelper.User) {
			// 仅检查是否登陆过。严谨些还应该检查是否有其他业务数据
			Object[] hasLogin = Application.createQueryNoFilter(
					"select count(logId) from LoginLog where user = ?")
					.setParameter(1, id)
					.unique();
			hasMember = ObjectUtils.toInt(hasLogin[0]);
		}

		JSONObject ret = JSONUtils.toJSONObject(new String[] { "hasMember", "hasChild" },
				new Object[] { hasMember, hasChild });
		writeSuccess(response, ret);
	}

	@RequestMapping( value = "user-delete", method = RequestMethod.POST)
	public void userDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getIdParameterNotNull(request, "id");
		Application.getBean(UserService.class).delete(user);
		writeSuccess(response);
	}
}