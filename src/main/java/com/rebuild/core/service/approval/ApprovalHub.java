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
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.rebuild.core.service.approval.ApprovalState.APPROVED;
import static com.rebuild.core.service.approval.ApprovalState.DRAFT;

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

    public static final int TYPE_REFERRAL = 1; // 转审
    public static final int TYPE_COUNTERSIGN = 2; // 加签

    // @see ApprovalState
    public static final int STATE_INVALID = 100;  // 作废了
    public static final int STATE_REFERRAL = 101;  // 转审了

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
        // 批次
        String hubBatch = CommonsUtils.randomHex(true);
        // 随便用一个即可
        ID aStepId = nextApprovalStepIds.iterator().next();
        ID recordId = (ID) QueryHelper.queryFieldValue(aStepId, "recordId");

        // 提交人
        Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
        r.setID("approvalStepId", aStepId);
        r.setID("recordId", recordId);
        r.setID("userSubmit", submitor);
        r.setString("hubBatch", hubBatch);
        records.add(r);

        // 审批节点
        records.addAll(buildApproves(nextApprovalStepIds, recordId, opUser, hubBatch));
        // 抄送
        records.addAll(buildCcs(aStepId, recordId, ccUsers, opUser, hubBatch, DRAFT.getState()));

        Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
    }

    /**
     * @param approvalStepId
     * @param nextApprovalStepIds
     * @param type 1=转审, 2=加签
     */
    public void awareApprove(ID approvalStepId, Collection<ID> nextApprovalStepIds, int type) {
        if (type == TYPE_REFERRAL) {
            Object[] approveHub = getApproveHub(approvalStepId);
            if (approveHub == null) return;

            ID opUser = UserContextHolder.getUser();
            Record r = EntityHelper.forUpdate((ID) approveHub[0], opUser);
            r.setInt("state", STATE_REFERRAL);
            r.setDate("approvedOn", CalendarUtils.now());
            Application.getCommonsService().update(r);

            List<Record> records = buildApproves(nextApprovalStepIds, (ID) approveHub[3], opUser, (String) approveHub[2]);
            if (!records.isEmpty()) {
                Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
            }
            return;
        }

        if (type == TYPE_COUNTERSIGN) {
            Object[] approveHub = getApproveHub(approvalStepId);
            if (approveHub == null) return;

            ID opUser = UserContextHolder.getUser();

            List<Record> records = buildApproves(nextApprovalStepIds, (ID) approveHub[3], opUser, (String) approveHub[2]);
            if (!records.isEmpty()) {
                Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
            }
            return;
        }

        awareApprove(approvalStepId, nextApprovalStepIds, null);
    }

    /**
     * @param approvalStepId
     * @param nextApprovalStepIds
     * @param ccUsers
     */
    public void awareApprove(ID approvalStepId, Collection<ID> nextApprovalStepIds, Collection<ID> ccUsers) {
        Object[] approveHub = getApproveHub(approvalStepId);
        if (approveHub == null) return;

        ID opUser = UserContextHolder.getUser();
        int approvalState = (Integer) approveHub[1];
        ID recordId = (ID) approveHub[3];

        // 转审的
        if ((Integer) approveHub[4] < 100) {
            Record r = EntityHelper.forUpdate((ID) approveHub[0], opUser);
            r.setInt("state", approvalState);
            r.setDate("approvedOn", CalendarUtils.now());
            Application.getCommonsService().update(r);
        }

        List<Record> records = new ArrayList<>();
        String hubBatch = (String) approveHub[2];

        // 处理作废节点
        Object[][] batchHubs = Application.createQueryNoFilter(
                "select hubId,state,userSubmit,userApprove,userCc,approvalStepId.isCanceled,approvalStepId.state from RobotApprovalHub where hubBatch = ? and state < 100")
                .setParameter(1, hubBatch)
                .array();
        int hasUserApprove = 0;
        int endUserApprove = 0;
        for (Object[] hub : batchHubs) {
            ID userApprove = (ID) hub[3];
            // 审批节点
            if (userApprove != null) {
                hasUserApprove++;

                // 已取消 || 已审核
                if ((Boolean) hub[5] || (Integer) hub[6] >= APPROVED.getState()) {
                    endUserApprove++;
                }
            }
        }

        // 本轮全部完成
        boolean isAllStepsEnd = hasUserApprove > 0 && hasUserApprove == endUserApprove;
        if (isAllStepsEnd) {
            for (Object[] hub : batchHubs) {
                if ((Integer) hub[1] == DRAFT.getState()) {
                    Record r = EntityHelper.forUpdate((ID) hub[0], opUser);
                    r.setDate("approvedOn", CalendarUtils.now());

                    ID userApprove = (ID) hub[3];
                    boolean isCanceled = (Boolean) hub[5];
                    if (userApprove != null && isCanceled) {
                        r.setInt("state", STATE_INVALID);
                    } else {
                        r.setInt("state", approvalState);
                    }

                    records.add(r);
                }
            }
        }

        // 新批次
        String hubBatchNext = CommonsUtils.randomHex(true);
        ID aNextStepId = CollectionUtils.isNotEmpty(nextApprovalStepIds) ? nextApprovalStepIds.iterator().next() : null;

        // 审批节点-如果没有后续审批，就使用最后一次的批次
        if (aNextStepId == null) {
            hubBatchNext = hubBatch;
        } else {
            records.addAll(buildApproves(nextApprovalStepIds, recordId, opUser, hubBatchNext));
        }

        // 抄送-如果没有后续审批，则抄送状态直接是完成
        ID ccStep = aNextStepId == null ? approvalStepId : aNextStepId;
        int ccState = aNextStepId == null ? APPROVED.getState() : DRAFT.getState();
        // 会签的没完成
        if (!isAllStepsEnd) ccState = DRAFT.getState();

        records.addAll(buildCcs(ccStep, recordId, ccUsers, opUser, hubBatchNext, ccState));

        if (!records.isEmpty()) {
            Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
        }
    }

    // 找到审批节点对应记录
    private Object[] getApproveHub(ID approvalStepId) {
        Object[] o = Application.createQueryNoFilter(
                "select hubId,approvalStepId.state,hubBatch,approvalStepId.recordId,state from RobotApprovalHub where approvalStepId = ? and userApprove is not null")
                .setParameter(1, approvalStepId)
                .unique();
        if (o == null) log.warn("No ApprovalHub for stepId: {}", approvalStepId);
        return o;
    }

    /**
     * @param approvalStepId
     */
    public void awareCancel(ID approvalStepId) {
        // 找到审批节点对应记录
        Object[] approveStep = Application.createQueryNoFilter(
                "select recordId,approvalId from RobotApprovalStep where stepId = ?")
                .setParameter(1, approvalStepId)
                .unique();
        if (approveStep == null) return;

        // 处理作废节点
        Object[][] cancelHubs = Application.createQueryNoFilter(
                "select hubId from RobotApprovalHub where state=1 and recordId=? and approvalStepId.approvalId=?")
                .setParameter(1, approveStep[0])
                .setParameter(2, approveStep[1])
                .array();

        ID opUser = UserContextHolder.getUser();
        List<Record> records = new ArrayList<>();

        for (Object[] hub : cancelHubs) {
            Record r = EntityHelper.forUpdate((ID) hub[0], opUser);
            r.setDate("approvedOn", CalendarUtils.now());
            r.setInt("state", ApprovalState.CANCELED.getState());  // 0=作废
            records.add(r);
        }

        if (!records.isEmpty()) {
            Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
        }
    }

    // 构造审批
    private List<Record> buildApproves(Collection<ID> nextApprovalStepIds, ID recordId, ID opUser, String hubBatch) {
        if (CollectionUtils.isEmpty(nextApprovalStepIds)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID step : nextApprovalStepIds) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
            r.setID("approvalStepId", step);
            r.setID("recordId", recordId);
            r.setID("userApprove", (ID) QueryHelper.queryFieldValue(step, "approver"));
            r.setString("hubBatch", hubBatch);
            records.add(r);
        }
        return records;
    }

    // 构造抄送
    private List<Record> buildCcs(ID approvalStepId, ID recordId, Collection<ID> ccUsers, ID opUser, String hubBatch, int state) {
        if (CollectionUtils.isEmpty(ccUsers)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID ccUser : ccUsers) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub, opUser);
            r.setID("approvalStepId", approvalStepId);
            r.setID("recordId", recordId);
            r.setID("userCc", ccUser);
            r.setString("hubBatch", hubBatch);
            r.setInt("state", state);
            records.add(r);
        }
        return records;
    }
}
