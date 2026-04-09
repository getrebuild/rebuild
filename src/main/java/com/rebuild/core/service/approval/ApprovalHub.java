/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 审批中心数据感知
 *
 * @author devezhao
 * @see ApprovalStepService
 * @since 2026/4/8
 */
@Slf4j
public class ApprovalHub {

    public static final ApprovalHub instance = new ApprovalHub();

    private ApprovalHub() {
    }

    /**
     * @param submitor
     * @param nextApprovalStepIds
     * @param ccUsers
     */
    public void awareSubmit(ID submitor, Collection<ID> nextApprovalStepIds, Collection<ID> ccUsers) {
        ID opUser = UserContextHolder.getUser();
        List<Record> records = new ArrayList<>();
        ID aStepId = nextApprovalStepIds.iterator().next();

        // 提交人
        Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
        r.setID("approvalStepId", aStepId);
        r.setID("userSubmit", submitor);
        Application.getCommonsService().create(r);

        // 审批节点
        records.addAll(buildApproves(nextApprovalStepIds, r.getPrimary(), opUser));
        // 抄送
        records.addAll(buildCcs(aStepId, ccUsers, r.getPrimary(), opUser));
        if (!records.isEmpty()) {
            Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
        }
    }

    /**
     * @param approvalStepId
     * @param nextApprovalStepIds
     * @param cancelApprovalStepIds
     * @param ccUsers
     */
    public void awareApprove(ID approvalStepId, Collection<ID> nextApprovalStepIds, Collection<ID> cancelApprovalStepIds, Collection<ID> ccUsers) {
        ID hubid = foundByStepId(approvalStepId);
        if (hubid == null) return;

        ID opUser = UserContextHolder.getUser();
        List<Record> records = new ArrayList<>();

        // 审批
        Record r = EntityHelper.forUpdate(hubid, opUser);
        r.setInt("state", (Integer) QueryHelper.queryFieldValue(approvalStepId, "state"));
        r.setDate("approvedOn", CalendarUtils.now());
        Application.getCommonsService().update(r);

        // 其他无效节点-删除???
        if (CollectionUtils.isNotEmpty(cancelApprovalStepIds)) {
            for (ID step : cancelApprovalStepIds) {
                hubid = foundByStepId(step);
                if (hubid != null) {
                    r = EntityHelper.forUpdate(hubid, opUser);
                    r.setInt("state", 0);
                    records.add(r);
                }
            }
        }

        // 审批节点
        records.addAll(buildApproves(nextApprovalStepIds, r.getPrimary(), opUser));
        // 抄送
        records.addAll(buildCcs(approvalStepId, ccUsers, r.getPrimary(), opUser));
        if (!records.isEmpty()) {
            Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
        }
    }

    // 审批
    List<Record> buildApproves(Collection<ID> nextApprovalStepIds, ID relatedHubId, ID opUser) {
        if (CollectionUtils.isEmpty(nextApprovalStepIds)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID step : nextApprovalStepIds) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
            r.setID("approvalStepId", step);
            r.setID("userApprove", (ID) QueryHelper.queryFieldValue(step, "approver"));
            if (relatedHubId != null) r.setID("relatedHubId", relatedHubId);
            records.add(r);
        }
        return records;
    }

    // 抄送
    List<Record> buildCcs(ID approvalStepId, Collection<ID> ccUsers, ID relatedHubId, ID opUser) {
        if (CollectionUtils.isEmpty(ccUsers)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID ccUser : ccUsers) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
            r.setID("approvalStepId", approvalStepId);
            r.setID("usercc", ccUser);
            if (relatedHubId != null) r.setID("relatedHubId", relatedHubId);
            records.add(r);
        }
        return records;
    }

    // 找到审批节点对应记录
    ID foundByStepId(ID approvalStepId) {
        Object[] hub = Application.createQueryNoFilter(
                "select hubId from RobotApprovalHub where approvalStepId = ?")
                .setParameter(1, approvalStepId)
                .unique();
        return hub == null ? null : (ID) hub[0];
    }

    // --

    /**
     * 查询步骤状态（会查询关联的）
     *
     * @param approvalStepId
     * @return
     */
    public int getApprovalStepState(ID approvalStepId) {
        Object[] step = Application.createQueryNoFilter(
                "select recordId,approvalId,nodeBatch from RobotApprovalStep where stepId = ?")
                .setParameter(1, approvalStepId)
                .unique();

        Object[][] steps = Application.createQueryNoFilter(
                "select state from RobotApprovalStep where recordId = ? and approvalId = ? and nodeBatch = ?")
                .setParameter(1, step[0])
                .setParameter(2, step[1])
                .setParameter(3, step[2])
                .array();

        return steps.length > 0 ? (Integer) steps[0][0] : 0;
    }
}
