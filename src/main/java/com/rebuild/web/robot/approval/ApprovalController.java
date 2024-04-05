/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.configuration.general.LiteFormBuilder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalProcessor;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStatus;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.approval.FlowDefinition;
import com.rebuild.core.service.approval.FlowNode;
import com.rebuild.core.service.approval.FlowNodeGroup;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Set;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/05
 */
@Slf4j
@RestController
@RequestMapping({"/app/entity/approval/", "/app/RobotApprovalConfig/"})
public class ApprovalController extends BaseController {

    @GetMapping("workable")
    public JSON getWorkable(HttpServletRequest request, @IdParam(name = "record") ID recordId) {
        final ID user = getRequestUser(request);

        FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(recordId, user);
        JSONArray res = new JSONArray();
        for (FlowDefinition d : defs) {
            res.add(d.toJSON("id", "name"));
        }

        // 名称排序
        res.sort((o1, o2) -> {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            return j1.getString("name").compareTo(j2.getString("name"));
        });
        return res;
    }

    @GetMapping("state")
    public RespBody getApprovalState(HttpServletRequest request, @IdParam(name = "record") ID recordId) {
        final Entity approvalEntity = MetadataHelper.getEntity(recordId.getEntityCode());
        if (!MetadataHelper.hasApprovalField(approvalEntity)) {
            return RespBody.error("NOT AN APPROVAL ENTITY");
        }

        final ID user = getRequestUser(request);
        final ApprovalStatus status = ApprovalHelper.getApprovalStatus(recordId);

        JSONObject data = new JSONObject();
        data.put("entityName", approvalEntity.getName());

        int stateVal = status.getCurrentState().getState();
        data.put("state", stateVal);

        ID useApproval = status.getApprovalId();
        if (useApproval != null) {
            data.put("approvalId", useApproval);
            // 审批中
            if (stateVal < ApprovalState.APPROVED.getState()) {
                JSONArray current = new ApprovalProcessor(recordId, useApproval).getCurrentStep(status);
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

            // 审批中提交人可撤回/催审
            if (stateVal == ApprovalState.PROCESSING.getState()) {
                if (user.equals(ApprovalHelper.getSubmitter(recordId, useApproval))) {
                    data.put("canUrge", true);
                    data.put("canCancel", true);
                } else if (UserHelper.isAdmin(user)) {
                    // v3.1 管理员也可撤回
                    data.put("canCancel", true);
                }
            }

            if (stateVal == ApprovalState.APPROVED.getState()) {
                // v3.4
                data.put("canRevoke", Application.getPrivilegesManager().allow(user, ZeroEntry.AllowRevokeApproval));
            }
        }

        return RespBody.ok(data);
    }

    @GetMapping("fetch-nextstep")
    public JSON fetchNextStep(HttpServletRequest request,
                              @IdParam(name = "record") ID recordId, @IdParam(name = "approval") ID approvalId) {
        final ID user = getRequestUser(request);

        ApprovalProcessor approvalProcessor = new ApprovalProcessor(recordId, approvalId);
        FlowNodeGroup nextNodes = approvalProcessor.getNextNodes();

        Set<ID> approverList = nextNodes.getApproveUsers(user, recordId, null);
        Set<ID> ccList = nextNodes.getCcUsers(user, recordId, null);

        // 自选审批人
        approverList.addAll(approvalProcessor.getSelfSelectedApprovers(nextNodes));

        JSONObject data = new JSONObject();
        data.put("nextApprovers", formatUsers(approverList));
        data.put("nextCcs", formatUsers(ccList));
        data.put("nextCcAccounts", nextNodes.getCcAccounts(recordId));
        data.put("approverSelfSelecting", nextNodes.allowSelfSelectingApprover());
        data.put("ccSelfSelecting", nextNodes.allowSelfSelectingCc());
        data.put("isLastStep", nextNodes.isLastStep());
        data.put("signMode", nextNodes.getSignMode());
        data.put("useGroup", nextNodes.getGroupId());
        // current
        final FlowNode currentFlowNode = approvalProcessor.getCurrentNode();
        data.put("isRejectStep", currentFlowNode.getRejectStep());
        data.put("currentNode", currentFlowNode.getNodeId());
        data.put("allowReferral", currentFlowNode.allowReferral());
        data.put("allowCountersign", currentFlowNode.allowCountersign());

        // 可修改字段
        JSONArray editableFields = currentFlowNode.getEditableFields();
        if (editableFields != null && !editableFields.isEmpty()) {
            JSONArray aform = new LiteFormBuilder(recordId, user).build(editableFields);
            if (aform != null && !aform.isEmpty()) {
                data.put("aform", aform);
                data.put("aentity", MetadataHelper.getEntityName(recordId));
            }
        }

        return data;
    }

    private JSONArray formatUsers(Collection<ID> users) {
        JSONArray array = new JSONArray();
        for (ID u : users) {
            array.add(new Object[] { u, UserHelper.getName(u) });
        }
        return array;
    }

    @GetMapping("fetch-workedsteps")
    public JSON fetchWorkedSteps(@IdParam(name = "record") ID recordId) {
        return new ApprovalProcessor(recordId).getWorkedSteps();
    }

    @GetMapping("fetch-backsteps")
    public JSON fetchBackSteps(@IdParam(name = "record") ID recordId) {
        return new ApprovalProcessor(recordId).getBackSteps();
    }

    @PostMapping("submit")
    public RespBody doSubmit(HttpServletRequest request,
                             @IdParam(name = "record") ID recordId, @IdParam(name = "approval") ID approvalId) {
        JSONObject selectUsers = (JSONObject) ServletUtils.getRequestJson(request);

        try {
            boolean success = new ApprovalProcessor(recordId, approvalId).submit(selectUsers);
            return success ? RespBody.ok() : RespBody.errorl("无效审批流程，请联系管理员配置");

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @PostMapping("approve")
    public RespBody doApprove(HttpServletRequest request, @IdParam(name = "record") ID recordId) {
        final ID approver = getRequestUser(request);
        final int state = getIntParameter(request, "state", ApprovalState.REJECTED.getState());
        final String rejectNode = getParameter(request, "rejectNode", null);

        JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        JSONObject selectUsers = post.getJSONObject("selectUsers");
        String remark = post.getString("remark");
        String useGroup = post.getString("useGroup");

        // 可编辑字段
        JSONObject aformData = post.getJSONObject("aformData");
        Record addedRecord = null;
        // 没有或无更新
        if (aformData != null && aformData.size() > 1) {
            try {
                addedRecord = EntityHelper.parse(aformData, getRequestUser(request));
            } catch (DataSpecificationException known) {
                log.warn(">>>>> {}", known.getLocalizedMessage());
                return RespBody.error(known.getLocalizedMessage());
            }
        }

        try {
            new ApprovalProcessor(recordId).approve(
                    approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers, addedRecord, useGroup, rejectNode, false);
            return RespBody.ok();

        } catch (DataSpecificationNoRollbackException ex) {
            return RespBody.error(ex.getLocalizedMessage(), DefinedException.CODE_APPROVE_WARN);
        } catch (ApprovalException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        } catch (UnexpectedRollbackException rolledback) {
            log.error("ROLLEDBACK", rolledback);
            return RespBody.error("ROLLEDBACK OCCURED");
        }
    }

    @RequestMapping("cancel")
    public RespBody doCancel(@IdParam(name = "record") ID recordId) {
        try {
            new ApprovalProcessor(recordId).cancel();
            return RespBody.ok();

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @RequestMapping("urge")
    public RespBody doUrge(@IdParam(name = "record") ID recordId) {
        int s = new ApprovalProcessor(recordId).urge();

        if (s == -1) {
            return RespBody.errorl("15 分钟内仅可催审一次");
        } else {
            return s > 0 ? RespBody.ok() : RespBody.errorl("无法发送催审通知");
        }
    }

    @RequestMapping("revoke")
    public RespBody doRevoke(@IdParam(name = "record") ID recordId) {
        try {
            new ApprovalProcessor(recordId).revoke();
            return RespBody.ok();

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @RequestMapping("referral")
    public RespBody doReferral(@IdParam(name = "record") ID recordId, @IdParam(name = "to") ID toUser, HttpServletRequest request) {
        try {
            new ApprovalProcessor(recordId).referral(getRequestUser(request), toUser);
            return RespBody.ok();

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @RequestMapping("countersign")
    public RespBody doCountersign(@IdParam(name = "record") ID recordId, HttpServletRequest request) {
        ID[] toUsers = getIdArrayParameter(request, "to");
        try {
            new ApprovalProcessor(recordId).countersign(getRequestUser(request), toUsers);
            return RespBody.ok();

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @GetMapping("flow-definition")
    public RespBody getFlowDefinition(@IdParam ID approvalId) {
        if (ApprovalStepService.APPROVAL_NOID.equals(approvalId)) {
            return RespBody.ok();
        }

        Object[] belongEntity = Application.createQueryNoFilter(
                "select belongEntity from RobotApprovalConfig where configId = ?")
                .setParameter(1, approvalId)
                .unique();
        if (belongEntity == null) {
            return RespBody.errorl("无效审批流程，可能已被删除");
        }

        FlowDefinition def = RobotApprovalManager.instance
                .getFlowDefinition(MetadataHelper.getEntity((String) belongEntity[0]), approvalId);

        JSONObject data = JSONUtils.toJSONObject(
                new String[] { "applyEntity", "flowDefinition" },
                new Object[] { belongEntity[0], def.getJSON("flowDefinition") });
        return RespBody.ok(data);
    }

    @GetMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String id) {
        ModelAndView mv = createModelAndView("/entity/approval/approval-view");
        mv.getModel().put("approvalId", id);
        return mv;
    }
}
