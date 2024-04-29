/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
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
        final Numerical[] nums = getNumericals();

        Numerical axis = nums[0];
        Object[] dataRaw = createQuery(buildSql(axis)).unique();
        JSONObject index = JSONUtils.toJSONObject(
                new String[]{"data", "label"},
                new Object[]{wrapAxisValue(axis, dataRaw[0], true), axis.getLabel()});

        // 对比
        if (nums.length > 1) {
            axis = nums[1];
            dataRaw = createQuery(buildSql(axis)).unique();
            index.put("data2", wrapAxisValue(axis, dataRaw[0], true));
            index.put("label2", axis.getLabel());
            index.put("ge", axis.getLabel());
        }

        return JSONUtils.toJSONObject("index", index);
    }

    private String buildSql(Numerical axis) {
        String sql = "select {0} from {1} where {2}";
        String where = getFilterSql();

        if (ParseHelper.validAdvFilter(axis.getFilter())) {
            AdvFilterParser filterParser = new AdvFilterParser(axis.getFilter());
            String fieldWhere = filterParser.toSqlWhere();
            if (fieldWhere != null) where = String.format("((%s) and (%s))", where, fieldWhere);
        }

        sql = MessageFormat.format(sql,
                axis.getSqlName(),
                getSourceEntity().getName(), where);
        return sql;
    }
}
