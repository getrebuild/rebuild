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

import cn.devezhao.persist4j.Record;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.configuration.RobotApprovalConfigService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.web.BasePageControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
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
				"select belongEntity,name,flowDefinition from RobotApprovalConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (config == null) {
			response.sendError(404, "审批流程不存在");
			return null;
		}
		
		Entity applyEntity = MetadataHelper.getEntity((String) config[0]);
		
		ModelAndView mv = createModelAndView("/admin/robot/approval-design.jsp");
		mv.getModel().put("configId", configId);
		mv.getModel().put("name", config[1]);
		mv.getModel().put("applyEntity", applyEntity.getName());
		mv.getModel().put("flowDefinition", config[2]);
		return mv;
	}
	
	@RequestMapping("approval/list")
	public void approvalList(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String belongEntity = getParameter(request, "entity");
		String sql = "select configId,name,belongEntity,belongEntity,isDisabled from RobotApprovalConfig where isDisabled = ?";
		if (StringUtils.isNotBlank(belongEntity)) {
			sql += " and belongEntity = '" + StringEscapeUtils.escapeSql(belongEntity) + "'";
		}
		sql += " order by name";

		boolean disabled = getBoolParameter(request, "disabled", false);
		Object[][] array = Application.createQuery(sql).setParameter(1, disabled).array();
		for (Object[] o : array) {
			o[3] = EasyMeta.getLabel(MetadataHelper.getEntity((String) o[3]));
		}
		writeSuccess(response, array);
	}

	@RequestMapping("approval/copy")
	public void copyApproval(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		String approvalName = getParameterNotNull(request, "name");
		ID father = getIdParameterNotNull(request, "father");
		boolean disableFather = getBoolParameter(request, "disabled", true);

		Object[] copy = Application.createQueryNoFilter(
				"select belongEntity,flowDefinition,isDisabled from RobotApprovalConfig where configId = ?")
				.setParameter(1, father)
				.unique();

		Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, user);
		record.setString("belongEntity", (String) copy[0]);
		record.setString("flowDefinition", (String) copy[1]);
		record.setString("name", approvalName);
		record = Application.getBean(RobotApprovalConfigService.class).create(record);

		if (disableFather && !(Boolean) copy[2]) {
			Record update = EntityHelper.forUpdate(father, user);
			update.setBoolean("isDisabled", true);
			Application.getBean(RobotApprovalConfigService.class).update(update);
		}
		writeSuccess(response, JSONUtils.toJSONObject("approvalId", record.getPrimary()));
	}
}
