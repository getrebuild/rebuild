/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.support.task.TaskExecutors;

import java.util.List;

/**
 * 业务实体用
 *
 * @author devezhao
 * @since 12/28/2018
 */
public interface EntityService extends ServiceSpec {

    /**
     * 删除（带级联）
     *
     * @param recordId
     * @param cascades 需要级联删除的实体
     * @return
     */
    int delete(ID recordId, String[] cascades);

    /**
     * 分配
     *
     * @param recordId
     * @param to
     * @param cascades 需要级联分配的实体
     * @return
     */
    int assign(ID recordId, ID to, String[] cascades);

    /**
     * 共享
     *
     * @param recordId
     * @param to
     * @param cascades
     * @return
     */
    default int share(ID recordId, ID to, String[] cascades) {
        return share(recordId, to, cascades, BizzPermission.READ.getMask());
    }

    /**
     * 共享
     *
     * @param recordId
     * @param to
     * @param cascades 需要级联分配的实体
     * @param rights 共享权限
     * @return
     */
    int share(ID recordId, ID to, String[] cascades, int rights);

    /**
     * 取消共享
     *
     * @param recordId   主记录
     * @param accessId 共享的 AccessID
     * @return
     */
    int unshare(ID recordId, ID accessId);

    /**
     * 批量操作
     *
     * @param context
     * @return
     */
    int bulk(BulkContext context);

    /**
     * 批量操作（异步）
     *
     * @param context
     * @return 任务 ID
     * @see TaskExecutors
     */
    String bulkAsync(BulkContext context);

    /**
     * 检查并获取（如有）重复记录
     *
     * @param checkRecord
     * @param limit 最大查重返回数量
     * @return
     */
    List<Record> getAndCheckRepeated(Record checkRecord, int limit);

    /**
     * 审批
     *
     * @param recordId
     * @param state 只接受通过或撤销
     * @param approvalUser 审批人
     * @see com.rebuild.core.service.approval.ApprovalStepService
     * @see com.rebuild.core.service.approval.ApprovalProcessor
     */
    void approve(ID recordId, ApprovalState state, ID approvalUser);
}
