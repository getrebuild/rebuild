/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalProcessor;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.ApprovalStatus;
import com.rebuild.core.service.approval.FlowDefinition;
import com.rebuild.core.service.approval.RobotApprovalManager;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批操作工具，支持提交、同意、驳回、撤回、撤销和查询审批状态
 *
 * @author devezhao
 * @since 2026/7/25
 */
@Slf4j
public class Approval implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String recordId = args.getString("record");
        if (StringUtils.isBlank(recordId) || !ID.isId(recordId)) {
            throw new ToolException("记录ID (record) 不能为空且需为有效ID");
        }

        ID record = ID.valueOf(recordId);
        String action = args.getString("action");
        if (StringUtils.isBlank(action)) {
            throw new ToolException("操作类型 (action) 不能为空，可选: submit, approve, reject, cancel, revoke, state");
        }

        switch (action.toLowerCase()) {
            case "submit":
                return doSubmit(record, args);
            case "approve":
                return doApprove(record, args, true);
            case "reject":
                return doApprove(record, args, false);
            case "cancel":
                return doCancel(record);
            case "revoke":
                return doRevoke(record);
            case "state":
                return doGetState(record);
            default:
                throw new ToolException("不支持的操作类型 (action): " + action + "，可选: submit, approve, reject, cancel, revoke, state");
        }
    }

    /**
     * 提交审批
     */
    private Object doSubmit(ID recordId, JSONObject args) throws Exception {
        final ID user = UserContextHolder.getUser();

        // 验证实体支持审批
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        if (!MetadataHelper.hasApprovalField(entity)) {
            throw new ToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不支持审批");
        }

        // 获取可用审批流程
        FlowDefinition[] defs = RobotApprovalManager.instance.getFlowDefinitions(recordId, user);
        if (defs.length == 0) {
            throw new ToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 未配置审批流程，无法提交审批。"
                    + "请管理员在「配置中心 - 审批流程」中为该实体配置审批流程");
        }

        ID approvalId;
        FlowDefinition useDef = defs[0];
        String approvalIdArg = args.getString("approvalId");
        if (StringUtils.isNotBlank(approvalIdArg) && ID.isId(approvalIdArg)) {
            approvalId = ID.valueOf(approvalIdArg);
            // 验证是否可用
            useDef = null;
            for (FlowDefinition d : defs) {
                if (d.getID("id").equals(approvalId)) {
                    useDef = d;
                    break;
                }
            }
            if (useDef == null) {
                throw new ToolException("指定的审批流程不可用或无权限: " + approvalIdArg);
            }
        } else {
            // 默认使用第一个可用流程
            approvalId = useDef.getID("id");
        }

        ApprovalProcessor processor = new ApprovalProcessor(recordId, approvalId);
        boolean success = processor.submit(null);

        if (success) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "approvalId", "approvalName", "message"},
                    new Object[]{"ok", approvalId.toLiteral(), useDef.getString("name"),
                            "已成功提交审批"});
        } else {
            throw new ToolException("提交审批失败，审批流程 [" + useDef.getString("name")
                    + "] 的首个审批节点未配置审批人，请管理员检查流程配置");
        }
    }

    /**
     * 同意/驳回
     */
    private Object doApprove(ID recordId, JSONObject args, boolean approve) throws Exception {
        final ID user = UserContextHolder.getUser();

        ApprovalStatus status = ApprovalHelper.getApprovalStatus(recordId);
        if (status.getCurrentState() != ApprovalState.PROCESSING) {
            throw new ToolException("记录当前审批状态为 [" + status.getCurrentState().getName() + "]，无法审批");
        }

        String remark = args.getString("remark");
        String rejectNode = args.getString("rejectNode");

        ApprovalState state = approve ? ApprovalState.APPROVED : ApprovalState.REJECTED;
        ApprovalProcessor processor = new ApprovalProcessor(recordId);

        try {
            processor.approve(
                    user, state,
                    new Object[]{remark, null},
                    null, null, null, rejectNode, false, false);

            String actionName = approve ? "同意" : "驳回";
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "审批已" + actionName});

        } catch (ApprovalException ex) {
            throw new ToolException(ex.getMessage(), ex);
        }
    }

    /**
     * 撤回（提交人撤回审批中的记录）
     */
    private Object doCancel(ID recordId) throws Exception {
        ApprovalProcessor processor = new ApprovalProcessor(recordId);
        try {
            processor.cancel();
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "审批已撤回"});
        } catch (ApprovalException ex) {
            throw new ToolException(ex.getMessage(), ex);
        }
    }

    /**
     * 撤销（撤销已通过的审批）
     */
    private Object doRevoke(ID recordId) throws Exception {
        ApprovalProcessor processor = new ApprovalProcessor(recordId);
        try {
            processor.revoke();
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "审批已撤销"});
        } catch (ApprovalException ex) {
            throw new ToolException(ex.getMessage(), ex);
        }
    }

    /**
     * 查询审批状态
     */
    private Object doGetState(ID recordId) throws Exception {
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        if (!MetadataHelper.hasApprovalField(entity)) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "实体 [" + EasyMetaFactory.getLabel(entity) + "] 不支持审批"});
        }

        ApprovalStatus status = ApprovalHelper.getApprovalStatus(recordId);
        ApprovalState state = status.getCurrentState();

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("state", state.getState());
        result.put("stateName", state.getName());
        result.put("approvalId", status.getApprovalId() == null ? null : status.getApprovalId().toLiteral());
        result.put("approvalName", status.getApprovalName());
        result.put("entityName", entity.getName());

        // 审批中时返回当前审批步骤
        if (state == ApprovalState.PROCESSING && status.getApprovalId() != null) {
            try {
                ApprovalProcessor processor = new ApprovalProcessor(recordId, status.getApprovalId());
                JSONArray currentStep = processor.getCurrentStep(status);
                List<String> approverNames = new ArrayList<>();
                for (Object o : currentStep) {
                    JSONObject step = (JSONObject) o;
                    String approver = step.getString("approver");
                    if (approver != null) {
                        approverNames.add(approver);
                    }
                }
                result.put("currentApprovers", approverNames);
            } catch (Exception warn) {
                log.warn("Error on getCurrentStep: {}", recordId, warn);
            }
        }

        return result;
    }
}
