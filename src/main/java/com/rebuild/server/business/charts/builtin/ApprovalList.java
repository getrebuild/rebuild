/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.charts.builtin;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalHelper;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

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
        return "我的审批";
    }

    @Override
    public JSONObject getChartConfig() {
        return JSONUtils.toJSONObject(new String[]{"entity", "type"}, new String[]{"User", getChartType()});
    }

    @Override
    public JSON build() {
        final int viewState = ObjectUtils.toInt(getExtraParams().get("state"), ApprovalState.DRAFT.getState());
        final String baseWhere = "where isCanceled = 'F' and isWaiting = 'F' and approver = ? and approvalId <> '' and approvalId is not null and ";

        Object[][] array = Application.createQueryNoFilter(
                "select createdBy,modifiedOn,recordId,approvalId from RobotApprovalStep " +
                        baseWhere + " state = ? order by modifiedOn desc")
                .setParameter(1, this.getUser())
                .setParameter(2, viewState)
                .setLimit(500)  // 最多显示
                .array();

        List<Object> rearray = new ArrayList<>();
        int deleted = 0;
        for (Object[] o : array) {
            final ID recordId = (ID) o[2];
            String label = null;
            try {
                label = FieldValueWrapper.getLabel(recordId);
            } catch (NoRecordFoundException ignored) {
                deleted++;
                continue;
            }

            Object[] states = ApprovalHelper.getApprovalStates(recordId);
            if ((Integer) states[2] == ApprovalState.CANCELED.getState()) {
                deleted++;
                continue;
            }

            ID s = ApprovalHelper.getSubmitter(recordId, (ID) o[3]);
            rearray.add(new Object[] {
                    s,
                    UserHelper.getName(s),
                    Moment.moment((Date) o[1]).fromNow(),
                    o[2],
                    label,
                    o[3],
                    MetadataHelper.getEntityLabel(recordId)
            });
        }

        Object[][] stats = Application.createQueryNoFilter(
                "select state,count(state) from RobotApprovalStep " + baseWhere + " state <> ? group by state")
                .setParameter(1, this.getUser())
                .setParameter(2, ApprovalState.CANCELED.getState())
                .array();
        // 排除删除的（可能导致不同状态下数据不一致）
        if (deleted > 0) {
            for (Object[] o : stats) {
                if ((Integer) o[0] == viewState) {
                    o[1] = ObjectUtils.toInt(o[1]) - deleted;
                    if ((Integer) o[1] < 0) {
                        o[1] = 0;
                    }
                }
            }
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("data", rearray);
        ret.put("stats", stats);
        return (JSON) JSON.toJSON(ret);
    }
}
