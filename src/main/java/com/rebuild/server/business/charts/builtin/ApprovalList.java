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

import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import java.util.Date;

/**
 * 审批列表
 *
 * @author devezhao
 * @since 2019/10/14
 */
public class ApprovalList extends ChartData implements BuiltinChart {

    public ApprovalList() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return ID.valueOf("017-0000000000000900");
    }

    @Override
    public String getChartTitle() {
        return "我的审批";
    }

    @Override
    public JSONObject getChartConfig() {
        return JSONUtils.toJSONObject(
                new String[]{"entity", "type"},
                new String[]{"RobotApprovalStep", getChartType()});
    }

    @Override
    public JSON build() {
        Object[][] array = Application.createQueryNoFilter(
                "select createdBy,createdBy,modifiedOn,recordId,recordId from RobotApprovalStep " +
                        "where isCanceled = 'F' and isWaiting = 'F' and state = ? and approver = ? order by modifiedOn desc")
                .setParameter(1, ApprovalState.DRAFT.getState())
                .setParameter(2, this.user)
                .array();
        for (Object[] o : array) {
            o[1] = UserHelper.getName((ID) o[0]);
            o[2] = Moment.moment((Date) o[2]).fromNow();
            o[4] = FieldValueWrapper.getLabelNotry((ID) o[4]);
        }

        return (JSON) JSON.toJSON(array);
    }
}
