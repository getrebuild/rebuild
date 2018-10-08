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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.Department;
import com.rebuild.server.helper.manager.DataListManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class UserControll extends BaseControll {
	
	@RequestMapping("users")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/bizuser/user-list.jsp", "User");
		JSON cfg = DataListManager.getColumnLayout("User");
		mv.getModel().put("DataListConfig", JSON.toJSONString(cfg));
		return mv;
	}
	
	@RequestMapping("dept-tree")
	public void deptTreeGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Object[][] firstDepts = Application.createQuery(
				"select deptId from Department where parentDept is null")
				.array();
		
		JSONArray firsts = new JSONArray();
		for (Object dept[] : firstDepts) {
			Department first = Application.getUserStore().getDept((ID) dept[0]);
			firsts.add(recursiveDeptTree(first));
		}
		writeSuccess(response, firsts);
	}
	
	/**
	 * 组织部门树
	 * 
	 * @param parent
	 */
	private static JSONObject recursiveDeptTree(Department parent) {
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
}