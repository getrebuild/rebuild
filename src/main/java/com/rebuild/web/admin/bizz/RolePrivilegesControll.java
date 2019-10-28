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

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class RolePrivilegesControll extends BaseEntityControll {

	@RequestMapping("role-privileges")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges.jsp", "Role", user);
		setEntities(mv);
		return mv;
	}
	
	@RequestMapping("role/{id}")
	public ModelAndView pagePrivileges(@PathVariable String id, HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ID roleId = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/admin/bizuser/role-privileges.jsp", "Role", user);
		setEntities(mv);
		mv.getModel().put("RoleId", roleId);
		return mv;
	}
	
	/**
	 * @param mv
	 */
	private void setEntities(ModelAndView mv) {
		List<Object[]> entities = new ArrayList<>();
		for (Entity e : MetadataSorter.sortEntities()) {
			if (EntityHelper.hasPrivilegesField(e)) {
				entities.add(new Object[] { e.getEntityCode(), EasyMeta.getLabel(e) });
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
		ID roleId = getIdParameterNotNull(request, "role");
		if (RoleService.ADMIN_ROLE.equals(roleId)) {
			writeFailure(response, "系统内建角色，不允许修改。管理员角色拥有系统最高级权限，请谨慎使用");
			return;
		}
		
		Object[][] array = Application.createQuery(
				"select entity,definition,zeroKey from RolePrivileges where roleId = ?")
				.setParameter(1, roleId)
				.array();
		for (Object[] o : array) {
			if ((int) o[0] == 0) {
				o[0] = o[2];
			}
		}
		
		JSON retJson = JSONUtils.toJSONArray(new String[] { "name", "definition" }, array);
		writeSuccess(response, retJson);
	}
	
	@RequestMapping( value = "privileges-update", method = RequestMethod.POST)
	public void privilegesUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON post = ServletUtils.getRequestJson(request);
		ID role = getIdParameterNotNull(request, "role");
		Application.getBean(RoleService.class).updatePrivileges(role, (JSONObject) post);
		writeSuccess(response);
	}
	
	@RequestMapping( value = "role-delete", method = RequestMethod.POST)
	public void roleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID role = getIdParameterNotNull(request, "id");
		ID transfer = getIdParameter(request, "transfer");  // TODO 转移到新角色
		
		Application.getBean(RoleService.class).deleteAndTransfer(role, transfer);
		writeSuccess(response);
	}
}
