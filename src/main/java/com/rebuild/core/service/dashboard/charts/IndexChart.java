/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.text.MessageFormat;

/**
 * 指标卡
 *
 * @author devezhao
 * @since 12/14/2018
 */
public class IndexChart extends ChartData {

    protected IndexChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Numerical[] nums = getNumericals();

        Numerical axis = nums[0];
        Object[] dataRaw = createQuery(buildSql(axis)).unique();

        JSONObject index = JSONUtils.toJSONObject(
                new String[]{"data", "label"},
                new Object[]{wrapAxisValue(axis, dataRaw[0], true), axis.getLabel()});

        return JSONUtils.toJSONObject("index", index);
    }

    private String buildSql(Numerical axis) {
        String sql = "select {0} from {1} where {2}";
        String where = getFilterSql();

        sql = MessageFormat.format(sql,
                axis.getSqlName(),
                getSourceEntity().getName(), where);
        return sql;
    }
}
