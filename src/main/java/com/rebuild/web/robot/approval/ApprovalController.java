/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.approval.*;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;
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
        if (!MetadataHelper.hasApprovalField(MetadataHelper.getEntity(recordId.getEntityCode()))) {
            return RespBody.error("NOT AN APPROVAL ENTITY");
        }

        final ID user = getRequestUser(request);
        final ApprovalStatus status = ApprovalHelper.getApprovalStatus(recordId);

        JSONObject data = new JSONObject();

        int stateVal = status.getCurrentState().getState();
        data.put("state", stateVal);

        ID useApproval = status.getApprovalId();
        if (useApproval != null) {
            data.put("approvalId", useApproval);
            // 审批中
            if (stateVal < ApprovalState.APPROVED.getState()) {
                JSONArray current = new ApprovalProcessor(recordId, useApproval).getCurrentStep(status.getCurrentStepNode());
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
            if (stateVal == ApprovalState.PROCESSING.getState()
                    && user.equals(ApprovalHelper.getSubmitter(recordId, useApproval))) {
                data.put("canCancel", true);
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
        data.put("approverSelfSelecting", nextNodes.allowSelfSelectingApprover());
        data.put("ccSelfSelecting", nextNodes.allowSelfSelectingCc());
        data.put("isLastStep", nextNodes.isLastStep());
        data.put("signMode", nextNodes.getSignMode());
        data.put("useGroup", nextNodes.getGroupId());
        // current
        data.put("isRejectStep", approvalProcessor.getCurrentNode().getRejectStep());
        data.put("currentNode", approvalProcessor.getCurrentNode().getNodeId());

        // 可修改字段
        JSONArray editableFields = approvalProcessor.getCurrentNode().getEditableFields();
        if (editableFields != null && !editableFields.isEmpty()) {
            JSONArray aform = new FormBuilder(recordId, user).build(editableFields);
            if (aform != null && !aform.isEmpty()) {
                data.put("aform", aform);
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
                    approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers, addedRecord, useGroup, rejectNode);
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
            return RespBody.errorl("5 分钟内仅可催审一次");
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
