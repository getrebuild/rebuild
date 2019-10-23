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

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalException;
import com.rebuild.server.business.approval.ApprovalHelper;
import com.rebuild.server.business.approval.ApprovalProcessor;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.approval.FlowNodeGroup;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/05
 */
@Controller
@RequestMapping({ "/app/entity/approval/", "/app/RobotApprovalConfig/" })
public class ApprovalControll extends BasePageControll {
	
	@RequestMapping("workable")
	public void getWorkable(HttpServletRequest request, HttpServletResponse response) {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		
		FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(recordId, user);
		JSONArray data = new JSONArray();
		for (FlowDefinition d : defs) {
			data.add(d.toJSON("id", "name"));
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("state")
	public void getApprovalState(HttpServletRequest request, HttpServletResponse response) {
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

			// 审批中提交人可撤销
			if (stateVal == ApprovalState.PROCESSING.getState()) {
				ID submitter = ApprovalHelper.getSubmitter(recordId);
				if (user.equals(submitter)) {
					data.put("canCancel", true);
				}
			}
		}
		writeSuccess(response, data);
	}
	
	@RequestMapping("fetch-nextstep")
	public void fetchNextStep(HttpServletRequest request, HttpServletResponse response) {
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
	
	@RequestMapping("fetch-workedsteps")
	public void fetchWorkedSteps(HttpServletRequest request, HttpServletResponse response) {
		ID recordId = getIdParameterNotNull(request, "record");
		ID user = getRequestUser(request);
		
		JSONArray allSteps = new ApprovalProcessor(user, recordId).getWorkedSteps();
		writeSuccess(response, allSteps);
	}
	
	@RequestMapping("submit")
	public void doSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID recordId = getIdParameterNotNull(request, "record");
		ID approvalId = getIdParameterNotNull(request, "approval");
		JSONObject selectUsers = (JSONObject) ServletUtils.getRequestJson(request);
		
		try {
			boolean success = new ApprovalProcessor(user, recordId, approvalId).submit(selectUsers);
			if (success) {
				writeSuccess(response);
			} else {
				writeFailure(response, "无效审批流程，请联系管理员配置");
			}
		} catch (ApprovalException ex) {
			writeFailure(response, ex.getMessage());
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
		
		try {
			new ApprovalProcessor(approver, recordId)
					.approve(approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers);
			writeSuccess(response);
		} catch (ApprovalException ex) {
			writeFailure(response, ex.getMessage());
		}
	}

	@RequestMapping("cancel")
	public void doCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID recordId = getIdParameterNotNull(request, "record");

		try {
			new ApprovalProcessor(user, recordId).cancel(null);
			writeSuccess(response);
		} catch (ApprovalException ex) {
			writeFailure(response, ex.getMessage());
		}
	}
	
	@RequestMapping("flow-definition")
	public void getFlowDefinition(HttpServletRequest request, HttpServletResponse response) {
		ID approvalId = getIdParameterNotNull(request, "id");
		Object[] belongEntity = Application.createQueryNoFilter(
				"select belongEntity from RobotApprovalConfig where configId = ?")
				.setParameter(1, approvalId)
				.unique();
		if (belongEntity == null) {
			writeFailure(response, "无效审批流程，可能已被删除");
			return;
		}
		
		FlowDefinition def = RobotApprovalManager.instance
				.getFlowDefinition(MetadataHelper.getEntity((String) belongEntity[0]), approvalId);
		
		JSONObject ret = JSONUtils.toJSONObject(
				new String[] { "applyEntity", "flowDefinition" },
				new Object[] { belongEntity[0], def.getJSON("flowDefinition") });
		writeSuccess(response, ret);
	}

	@RequestMapping("view/{id}")
	public ModelAndView pageView(@PathVariable String id) {
		ModelAndView mv = createModelAndView("/entity/approval/approval-view.jsp");
		mv.getModel().put("approvalId", id);
		return mv;
	}
}
