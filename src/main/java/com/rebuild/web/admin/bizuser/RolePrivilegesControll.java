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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.RoleService;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.PortalMetaSorter;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class RolePrivilegesControll extends BaseControll {

	@RequestMapping("role-privileges")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges.jsp", "Role");
		setEntities(mv);
		return mv;
	}
	
	@RequestMapping("role/{id}")
	public ModelAndView pagePrivileges(@PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges.jsp", "Role");
		setEntities(mv);
		
		ID roleId = ID.valueOf(id);
		mv.getModel().put("RoleId", roleId.toLiteral());
		return mv;
	}
	
	/**
	 * @param mv
	 */
	private void setEntities(ModelAndView mv) {
		List<String[]> entities = new ArrayList<>();
		for (Entity e : PortalMetaSorter.sortEntities(true)) {
			if (EntityHelper.hasPrivilegesField(e)) {
				entities.add(new String[] { e.getName(), EasyMeta.getLabel(e) });
			}
		}
		mv.getModel().put("Entities", entities);
	}
	
	@RequestMapping("role-list")
	public void roleList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Object[][] array = Application.createQuery("select roleId,name,isDisabled from Role").array();
		JSON retJson = JSONUtils.toJSONArray(new String[] { "id", "name", "disabled" }, array);
		writeSuccess(response, retJson);
	}
	
	@RequestMapping("privileges-list")
	public void privilegesList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID role = getIdParameterNotNull(request, "role");
		if (RoleService.ADMIN_ROLE.equals(role)) {
			writeFailure(response, "管理员权限不允许修改");
			return;
		}
		
		Object[][] array = Application.createQuery(
				"select entity,definition,zeroKey from RolePrivileges where roleId = ?")
				.setParameter(1, role)
				.array();
		for (Object[] o : array) {
			String entity = o[0].toString();
			if ("N".equals(entity)) {
				o[0] = o[2];
			}
		}
		
		JSON retJson = JSONUtils.toJSONArray(new String[] { "name", "definition" }, array);
		writeSuccess(response, retJson);
	}
	
	@RequestMapping("privileges-update")
	public void privilegesUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON post = ServletUtils.getRequestJson(request);
		ID role = getIdParameterNotNull(request, "role");
		Application.getBean(RoleService.class).batchUpdatePrivileges(role, (JSONObject) post);
		writeSuccess(response);
	}
}
