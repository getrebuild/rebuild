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
import com.rebuild.server.business.approval.FlowNodeGroup;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
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
				EntityHelper.ApprovalId, EntityHelper.ApprovalState);
		if (state == null) {
			writeFailure(response, "无效记录");
			return;
		}
		
		Map<String, Object> data = new HashMap<>();

		int stateVal = ObjectUtils.toInt(state[1], ApprovalState.DRAFT.getState());
		data.put("state", stateVal);
		ID useApproval = (ID) state[0];
		if (useApproval != null) {
			data.put("approvalId", useApproval);
			// 当前审批步骤
			if (stateVal < ApprovalState.APPROVED.getState()) {
				JSONArray current = new ApprovalProcessor(user, recordId, useApproval).getCurrentStep();
				data.put("currentStep", current);
				
				for (Object o : current) {
					JSONObject step = (JSONObject) o;
					if (user.toLiteral().equalsIgnoreCase(step.getString("approver"))) {
						data.put("imApprover", true);
						data.put("imApproveSatate", step.getInteger("state"));
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
		
		FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(recordId, user);
		JSONArray data = new JSONArray();
		for (FlowDefinition d : defs) {
			data.add(d.toJSON("id", "name"));
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("fetch-nextstep")
	public void fetchNextStep(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameterNotNull(request, "record");
		ID approvalId = getIdParameterNotNull(request, "approval");
		ID user = getRequestUser(request);
		
		ApprovalProcessor approvalProcessor = new ApprovalProcessor(user, recordId, approvalId);
		FlowNodeGroup nextNodes = approvalProcessor.getNextNodes();
		
		JSONArray approverList = new JSONArray();
		for (ID o : nextNodes.getApproveUsers(user, recordId, null)) {
			approverList.add(new Object[] { o, UserHelper.getName(o) });
		}
		JSONArray ccList = new JSONArray();
		for (ID o : nextNodes.getCcUsers(user, recordId, null)) {
			ccList.add(new Object[] { o, UserHelper.getName(o) });
		}
		
		JSONObject data = new JSONObject();
		data.put("nextApprovers", approverList);
		data.put("nextCcs", ccList);
		data.put("approverSelfSelecting", nextNodes.allowSelfSelectingApprover());
		data.put("ccSelfSelecting", nextNodes.allowSelfSelectingCc());
		data.put("isLastStep", nextNodes.isLastStep());
		data.put("signMode", nextNodes.getSignMode());
		writeSuccess(response, data);
	}
	
	@RequestMapping("submit")
	public void doSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID recordId = getIdParameterNotNull(request, "record");
		ID approvalId = getIdParameterNotNull(request, "approval");
		JSONObject selectUsers = (JSONObject) ServletUtils.getRequestJson(request);
		
		boolean success = new ApprovalProcessor(user, recordId, approvalId).submit(selectUsers);
		if (success) {
			writeSuccess(response);
		} else {
			writeFailure(response, "无效审批流程，请联系管理员配置");
		}
	}
	
	@RequestMapping("approve")
	public void doApprove(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID approver = getRequestUser(request);
		ID recordId = getIdParameterNotNull(request, "record");
		int state = getIntParameter(request, "state", ApprovalState.REJECTED.getState());
		
		JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
		JSONObject selectUsers = post.getJSONObject("selectUsers");
		String remark = post.getString("remark");
		
		boolean success = new ApprovalProcessor(approver, recordId)
				.approve(approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers);
		if (success) {
			writeSuccess(response);
		} else {
			writeFailure(response, "无效审批状态");
		}
	}
}
