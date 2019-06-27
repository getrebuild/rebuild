/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.robot;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.web.BasePageControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
@Controller
@RequestMapping("/admin/robot/")
public class RobotApprovalControll extends BasePageControll {

	@RequestMapping("approvals")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/robot/approval-list.jsp");
		return mv;
	}
	
	@RequestMapping("approval/{id}")
	public ModelAndView pageEditor(@PathVariable String id, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID configId = ID.valueOf(id);
		Object[] config = Application.createQuery(
				"select belongEntity,name from RobotApprovalConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (config == null) {
			response.sendError(404, "审核流程不存在");
			return null;
		}
		
		Entity applyEntity = MetadataHelper.getEntity((String) config[0]);
		
		ModelAndView mv = createModelAndView("/admin/robot/approval-design.jsp");
		mv.getModel().put("configId", configId);
		mv.getModel().put("name", config[1]);
		mv.getModel().put("applyEntity", applyEntity.getName());
		mv.getModel().put("applyEntityLabel", EasyMeta.getLabel(applyEntity));
		return mv;
	}
	
	@RequestMapping("approval/list")
	public void approvalList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String belongEntity = getParameter(request, "entity");
		String sql = "select configId,name,belongEntity,belongEntity from RobotApprovalConfig";
		if (StringUtils.isNotBlank(belongEntity)) {
			sql += " where belongEntity = '" + StringEscapeUtils.escapeSql(belongEntity) + "'";
		}
		sql += " order by name";
		
		Object[][] array = Application.createQuery(sql).array();
		for (Object[] o : array) {
			o[3] = EasyMeta.getLabel(MetadataHelper.getEntity((String) o[3]));
		}
		writeSuccess(response, array);
	}
}
