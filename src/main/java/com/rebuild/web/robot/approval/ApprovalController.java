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
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalProcessor;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStatus;
import com.rebuild.core.service.approval.ApprovalStepService;
import com.rebuild.core.service.approval.EditableFields;
import com.rebuild.core.service.approval.FlowDefinition;
import com.rebuild.core.service.approval.FlowNode;
import com.rebuild.core.service.approval.FlowNodeGroup;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.trigger.DataValidateException;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.rebuild.core.privileges.bizz.ZeroEntry.AllowRevokeApproval;

/**
 * @author devezhao
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

    @GetMapping("alist")
    public RespBody getApprovalList(HttpServletRequest request, @EntityParam Entity entity) {
        boolean valid = getBoolParameter(request, "valid");

        FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(entity);
        List<Object> res = new ArrayList<>();
        for (FlowDefinition d : defs) {
            if (d.isDisabled()) continue;
            // 仅返回可用的
            if (valid && !d.isWorkable()) continue;

            res.add(JSONUtils.toJSONObject(new String[]{"id", "text"},
                    new Object[]{d.getID("id"), d.getString("name")}));
        }
        return RespBody.ok(res);
    }

    @GetMapping("state")
    public RespBody getApprovalState(HttpServletRequest request, @IdParam(name = "record") ID recordId) {
        final Entity approvalEntity = MetadataHelper.getEntity(recordId.getEntityCode());
        if (!MetadataHelper.hasApprovalField(approvalEntity)) {
            return RespBody.error("NONE APPROVAL ENTITY");
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
                    // v4.1 提交后有审批了
                    if (!"ROOT".equalsIgnoreCase(status.getPrevStepNode())) {
                        FlowNode root = ApprovalHelper.getFlowNode(useApproval, FlowNode.NODE_ROOT);
                        if (root != null && root.getDataMap().getBooleanValue("unallowCancel")) {
                            data.put("canCancel", false);
                        }
                    }
                }
                if (Application.getPrivilegesManager().allow(user, AllowRevokeApproval)) {
                    // v3.1 管理员可撤回
                    // v3.8 有权限的可撤回
                    data.put("canCancel", true);
                }

                // v3.8 自己审批的自己可以取消（退回）
                Set<ID> us = new ApprovalProcessor(recordId, useApproval).getPrevApprovedUsers();
                if (us.contains(user)) data.put("canCancel38", true);
            }

            if (stateVal == ApprovalState.APPROVED.getState()) {
                // v3.4 有权限的可撤销
                data.put("canRevoke", Application.getPrivilegesManager().allow(user, AllowRevokeApproval));
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
        // v4.0
        Long expTime = currentFlowNode.getExpiresTime(recordId, user);
        data.put("expiresTime", expTime);
        // 0=选填, 1=必填, 2=超时必填
        int reqType = currentFlowNode.getDataMap().getIntValue("remarkReq");
        if (reqType < 2) data.put("remarkReq", reqType);
        else data.put("remarkReq", expTime == null || expTime < 0 ? 0 : 1);

        // 可修改记录
        int editableMode = currentFlowNode.getEditableMode();
        data.put("editableMode", editableMode);
        if (editableMode ==FlowNode.EDITABLE_MODE_FIELDS) {
            JSONArray editableFields = currentFlowNode.getEditableFields();
            if (!CollectionUtils.isEmpty(editableFields)) {
                data.putAll(new EditableFields(editableFields).buildForms(recordId, user));
            }
        }

        return data;
    }

    private JSONArray formatUsers(Collection<ID> users) {
        JSONArray array = new JSONArray();
        for (ID u : users) array.add(new Object[]{u, UserHelper.getName(u)});
        return array;
    }

    @GetMapping("fetch-workedsteps")
    public JSON fetchWorkedSteps(@IdParam(name = "record") ID recordId, HttpServletRequest request) {
        return new ApprovalProcessor(recordId).getWorkedSteps(getBoolParameter(request, "his"));
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
        JSONArray aformData = post.getJSONArray("aformData");
        Record addedRecord = null;
        // v3.9 弱校验
        final ID weakMode = getIdParameter(request, "weakMode");
        if (CollectionUtils.isNotEmpty(aformData)) {
            List<Record> details = new ArrayList<>();
            try {
                for (Object o : aformData) {
                    Record a = EntityHelper.parse((JSONObject) o, approver);
                    if (a.getEntity().getEntityCode().equals(recordId.getEntityCode())) addedRecord = a;
                    else details.add(a);
                }

                if (addedRecord == null) addedRecord = EntityHelper.forUpdate(recordId, approver);
                if (!details.isEmpty()) addedRecord.setObjectValue(GeneralEntityService.HAS_DETAILS, details);

            } catch (DataSpecificationException known) {
                log.warn(">>>>> {}", known.getLocalizedMessage());
                return RespBody.error(known.getLocalizedMessage());
            }

            if (!Application.getEntityService(addedRecord.getEntity().getEntityCode()).getAndCheckRepeated(addedRecord, 1).isEmpty()) {
                return RespBody.errorl("存在重复记录");
            }
            if (weakMode != null) RbvFunction.call().setWeakMode(weakMode);
        }

        try {
            new ApprovalProcessor(recordId).approve(
                    approver, (ApprovalState) ApprovalState.valueOf(state), remark, selectUsers, addedRecord, useGroup, rejectNode, false);
            return RespBody.ok();

        } catch (DataSpecificationNoRollbackException ex) {
            return RespBody.error(ex.getLocalizedMessage(), DefinedException.CODE_APPROVE_WARN);
        } catch (ApprovalException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        } catch (DataValidateException known) {
            if (known.isWeakMode()) {
                String msg = known.getLocalizedMessage() + "$$$$" + known.getWeakModeTriggerId();
                return RespBody.error(msg, DefinedException.CODE_WEAK_VALIDATE);
            }
            return RespBody.error(known.getLocalizedMessage());
        } catch (UnexpectedRollbackException rolledback) {
            log.error("ROLLEDBACK", rolledback);
            return RespBody.error("ROLLEDBACK OCCURED");
        } finally {
            if (weakMode != null) RbvFunction.call().getWeakMode(true);
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

    @RequestMapping("cancel38")
    public RespBody doCancel38(@IdParam(name = "record") ID recordId, HttpServletRequest request) {
        try {
            new ApprovalProcessor(recordId).cancel38(getRequestUser(request));
            return RespBody.ok();

        } catch (ApprovalException ex) {
            return RespBody.error(ex.getMessage());
        }
    }

    @RequestMapping("urge")
    public RespBody doUrge(@IdParam(name = "record") ID recordId) {
        int s = new ApprovalProcessor(recordId).urge();

        if (s == -1) {
            return RespBody.errorl("催审通知发送过于频繁，请稍后重试");
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

        Entity applyEntity = MetadataHelper.getEntity((String) belongEntity[0]);
        FlowDefinition def = RobotApprovalManager.instance.getFlowDefinition(applyEntity, approvalId);
        JSONObject data = JSONUtils.toJSONObject(
                new String[]{"applyEntity", "flowDefinition"},
                new Object[]{applyEntity.getName(), def.getJSON("flowDefinition")});
        return RespBody.ok(data);
    }

    @GetMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String id) {
        ModelAndView mv = createModelAndView("/entity/approval/approval-view");
        mv.getModel().put("approvalId", id);
        return mv;
    }
}
