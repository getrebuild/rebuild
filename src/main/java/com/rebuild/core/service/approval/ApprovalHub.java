/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.query.QueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

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
        final List<Record> records = new ArrayList<>();
        final ID aStepId = nextApprovalStepIds.iterator().next();

        // 提交人
        Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub);
        r.setID("approvalStepId", aStepId);
        r.setID("userSubmit", submitor);
        records.add(r);

        // 审批节点
        records.addAll(buildApproves(nextApprovalStepIds));
        // 抄送
        records.addAll(buildCcs(aStepId, ccUsers));

        Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
    }

    /**
     * @param approvalStepId
     * @param nextApprovalStepIds
     * @param cancelApprovalStepIds
     * @param ccUsers
     */
    public void awareApprove(ID approvalStepId, Collection<ID> nextApprovalStepIds, Collection<ID> cancelApprovalStepIds, Collection<ID> ccUsers) {
        List<Record> records = new ArrayList<>();

        // 审批
        Record r = EntityHelper.forUpdate(foundByStepId(approvalStepId));
        r.setInt("state", (Integer) QueryHelper.queryFieldValue(approvalStepId, "state"));
        records.add(r);

        // 其他无效节点
        if (CollectionUtils.isNotEmpty(cancelApprovalStepIds)) {
            for (ID step : cancelApprovalStepIds) {
                r = EntityHelper.forUpdate(foundByStepId(step));
                r.setInt("state", 0);
            }
        }

        // 审批节点
        records.addAll(buildApproves(nextApprovalStepIds));
        // 抄送
        records.addAll(buildCcs(approvalStepId, ccUsers));

        Application.getCommonsService().createOrUpdate(records.toArray(new Record[0]));
    }

    // 审批
    List<Record> buildApproves(Collection<ID> nextApprovalStepIds) {
        if (CollectionUtils.isEmpty(nextApprovalStepIds)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID step : nextApprovalStepIds) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub);
            r.setID("approvalStepId", step);
            r.setID("userApprove", (ID) QueryHelper.queryFieldValue(step, "approver"));
            records.add(r);
        }
        return records;
    }

    // 抄送
    List<Record> buildCcs(ID approvalStepId, Collection<ID> ccUsers) {
        if (CollectionUtils.isEmpty(ccUsers)) return Collections.emptyList();

        List<Record> records = new ArrayList<>();
        for (ID ccUser : ccUsers) {
            Record r = EntityHelper.forNew(EntityHelper.RobotApprovalHub);
            r.setID("approvalStepId", approvalStepId);
            r.setID("usercc", ccUser);
            records.add(r);
        }
        return records;
    }

    // 找到审批节点对应记录
    ID foundByStepId(ID approvalStepId) {
        Object[] id = Application.createQueryNoFilter(
                        "select hubId from RobotApprovalHub where approvalStepId = ?")
                .setParameter(1, approvalStepId)
                .unique();
        Assert.notNull(id, "Not found RobotApprovalHub by step : " + approvalStepId);
        return (ID) id[0];
    }
}
