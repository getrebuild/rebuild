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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 审批列表
 *
 * @author devezhao
 * @since 2019/10/14
 */
public class ApprovalList implements BuiltinChart {

    public static final ID CHART_ID = ID.valueOf("017-0000000000000900");
    public static final String CHART_TITLE = "我的审批";
    public static final JSONObject CHART_CONFIG = JSONObject.parseObject("{ entity:'RobotApprovalStep', type:'ApprovalList' }");

    @Override
    public ID getChartId() {
        return CHART_ID;
    }

    @Override
    public String getChartTitle() {
        return CHART_TITLE;
    }

    @Override
    public JSONObject getChartConfig() {
        return CHART_CONFIG;
    }

    @Override
    public JSON build() {
        return null;
    }
}
