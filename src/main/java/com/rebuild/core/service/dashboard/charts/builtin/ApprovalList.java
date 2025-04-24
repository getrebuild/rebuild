/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalProcessor;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.FlowNode;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审批列表/统计
 *
 * @author devezhao
 * @since 2019/10/14
 */
@Slf4j
public class ApprovalList extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000001");

    public ApprovalList() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("我的审批");
    }

    @Override
    public JSON build() {
        // FIXME 存在一定性能问题
        JSONObject data = new JSONObject();
        data.put("state1", queryByState(ApprovalState.DRAFT.getState()));
        data.put("state10", queryByState(ApprovalState.APPROVED.getState()));
        data.put("state11", queryByState(ApprovalState.REJECTED.getState()));
        return data;
    }

    protected List<Object[]> queryByState(int viewState) {
        String sql = "select createdBy,modifiedOn,recordId,approvalId from RobotApprovalStep" +
                " where isCanceled = 'F' and isWaiting = 'F' and approver = ? and approvalId <> '' and recordId <> '' and state = ?" +
                " order by modifiedOn desc";
        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, getUser())
                .setParameter(2, viewState)
                .setLimit(500)  // 最多显示
                .array();

        List<Object[]> stateList = new ArrayList<>();
        for (Object[] o : array) {
            final ID recordId = (ID) o[2];
            String label;
            try {
                label = FieldValueHelper.getLabel(recordId);
            } catch (NoRecordFoundException ignored) {
                // 已删除
                continue;
            }

            // 已取消
            ApprovalState currentState = ApprovalHelper.getApprovalState(recordId);
            if (currentState == ApprovalState.CANCELED) {
                continue;
            }

            FlowNode currentNode = null;
            if (viewState == ApprovalState.PROCESSING.getState()) {
                try {
                    ApprovalProcessor approvalProcessor = new ApprovalProcessor(recordId, (ID) o[3]);
                    currentNode = approvalProcessor.getCurrentNode();
                } catch (Exception warn) {
                    log.warn("Error on `getCurrentNode` : {}", recordId);
                }
            }

            Entity e = MetadataHelper.getEntity(recordId.getEntityCode());
            ID s = ApprovalHelper.getSubmitter(recordId, (ID) o[3]);
            stateList.add(new Object[]{
                    s,
                    UserHelper.getName(s),
                    I18nUtils.formatDate((Date) o[1]),
                    o[2],
                    label,
                    o[3],
                    EasyMetaFactory.getLabel(e),
                    e.getName(),
                    currentNode == null ? null : currentNode.getExpiresTime(recordId, getUser())
            });
        }

        return stateList;
    }
}
