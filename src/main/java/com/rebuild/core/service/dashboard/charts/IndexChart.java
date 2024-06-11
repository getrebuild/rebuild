/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

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

        Numerical num = nums[0];
        Object[] dataRaw = createQuery(buildSql(num, true)).unique();
        JSONObject index = JSONUtils.toJSONObject(
                new String[]{"data", "label"},
                new Object[]{wrapAxisValue(num, dataRaw[0], true), num.getLabel()});

        // 对比
        if (nums.length > 1) {
            num = nums[1];
            dataRaw = createQuery(buildSql(num, true)).unique();
            index.put("data2", wrapAxisValue(num, dataRaw[0], true));
            index.put("label2", num.getLabel());
        }

        return JSONUtils.toJSONObject("index", index);
    }
}
