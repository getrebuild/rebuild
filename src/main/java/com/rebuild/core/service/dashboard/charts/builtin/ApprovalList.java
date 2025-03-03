/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final int viewState = ObjectUtils.toInt(getExtraParams().get("state"), ApprovalState.DRAFT.getState());
        final String baseWhere = "where isCanceled = 'F' and isWaiting = 'F' and approver = ?" +
                " and approvalId <> '' and recordId <> '' and ";

        Object[][] array = Application.createQueryNoFilter(
                "select createdBy,modifiedOn,recordId,approvalId from RobotApprovalStep " +
                        baseWhere + " state = ? order by modifiedOn desc")
                .setParameter(1, getUser())
                .setParameter(2, viewState)
                .setLimit(500)  // 最多显示
                .array();

        List<Object> rearray = new ArrayList<>();
        int removed = 0;
        for (Object[] o : array) {
            final ID recordId = (ID) o[2];
            String label;
            try {
                label = FieldValueHelper.getLabel(recordId);
            } catch (NoRecordFoundException ignored) {
                removed++;
                continue;
            }

            final ApprovalState currentState = ApprovalHelper.getApprovalState(recordId);
            if (currentState == ApprovalState.CANCELED) {
                removed++;
                continue;
            }

            FlowNode currentNode = null;
            if (currentState == ApprovalState.PROCESSING) {
                try {
                    ApprovalProcessor approvalProcessor = new ApprovalProcessor(recordId, (ID) o[3]);
                    currentNode = approvalProcessor.getCurrentNode();
                } catch (Exception warn) {
                    log.warn("Error on `getCurrentNode` : {}", recordId);
                }
            }

            Entity e = MetadataHelper.getEntity(recordId.getEntityCode());
            ID s = ApprovalHelper.getSubmitter(recordId, (ID) o[3]);
            rearray.add(new Object[]{
                    s,
                    UserHelper.getName(s),
                    I18nUtils.formatDate((Date) o[1]),
                    o[2],
                    label,
                    o[3],
                    EasyMetaFactory.getLabel(e),
                    e.getName(),
                    currentNode == null ? 0 : currentNode.getRemarkReq(recordId, getUser())
            });
        }

        Object[][] stats = Application.createQueryNoFilter(
                "select state,count(state) from RobotApprovalStep " + baseWhere + " state < ? group by state")
                .setParameter(1, this.getUser())
                .setParameter(2, ApprovalState.CANCELED.getState())
                .array();
        // 排除删除和无效的（可能导致不同状态下数据不一致）
        if (removed > 0) {
            for (Object[] o : stats) {
                if ((Integer) o[0] == viewState) {
                    o[1] = ObjectUtils.toInt(o[1]) - removed;
                    if ((Integer) o[1] < 0) {
                        o[1] = 0;
                    }
                }
            }
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("data", rearray);
        ret.put("stats", stats);
        ret.put("overLimit", array.length >= 500);
        return (JSON) JSON.toJSON(ret);
    }
}
