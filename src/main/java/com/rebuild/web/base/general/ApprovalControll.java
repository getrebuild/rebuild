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

package com.rebuild.web.base.general;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalProcessor;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/05
 */
@Controller
@RequestMapping("/app/entity/approval/")
public class ApprovalControll extends BaseControll {
	
	@RequestMapping("state")
	public void getApprovalState(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		
		Object[] state = Application.getQueryFactory().unique(recordId,
				EntityHelper.ApprovalId, EntityHelper.ApprovalState, EntityHelper.ApprovalStepNode);
		if (state == null) {
			writeFailure(response, "无效记录");
			return;
		}
		
		Map<String, Object> data = new HashMap<>();

		int stateVal = ObjectUtils.toInt(state[1], ApprovalState.DRAFT.getState());
		data.put("state", stateVal);
		if (state[0] != null) {
			data.put("approvalId", state[0]);
			JSONArray steps = new ApprovalProcessor(user, recordId, (ID) state[0]).getWorkedSteps();
			data.put("steps", steps);
			
			// 当前审批步骤
			if (stateVal < ApprovalState.APPROVED.getState() && !steps.isEmpty()) {
				JSONArray currentSteps = (JSONArray) steps.get(steps.size() - 1);
				for (Object o : currentSteps) {
					JSONObject cs = (JSONObject) o;
					if (user.toLiteral().equalsIgnoreCase(cs.getString("approver"))) {
						data.put("imApprover", cs.getInteger("state"));
						break;
					}
				}
			}
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("workable")
	public void getWorkable(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		
		FlowDefinition[] defs = RobotApprovalManager.instance
				.getFlowDefinitions(MetadataHelper.getEntity(recordId.getEntityCode()), user);
		JSONArray data = new JSONArray();
		for (FlowDefinition d : defs) {
			data.add(d.toJSON("id", "name"));
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("submit")
	public void doSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID approvalId = getIdParameterNotNull(request, "approval");
		ID user = getRequestUser(request);
		
		boolean success = new ApprovalProcessor(user, recordId, approvalId).submit();
		if (success) {
			writeSuccess(response);
		} else {
			writeFailure(response, "无效审批流程");
		}
	}
	
	@RequestMapping("approved")
	public void doApproved(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		String remark = ServletUtils.getRequestString(request);
	}
	
	@RequestMapping("rejected")
	public void doRejected(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		String remark = ServletUtils.getRequestString(request);
	}
}
