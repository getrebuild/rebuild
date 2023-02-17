/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.util.Collections;
import java.util.List;

/**
 * 饼图
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class PieChart extends ChartData {

    protected PieChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Dimension dim1 = dims[0];
        Numerical num1 = nums[0];
        Object[][] dataRaw = createQuery(buildSql(dim1, num1)).array();

        JSONArray data = new JSONArray();
        for (Object[] o : dataRaw) {
            o[0] = wrapAxisValue(dim1, o[0]);
            o[1] = wrapAxisValue(num1, o[1]);
            JSON d = JSONUtils.toJSONObject(new String[]{"name", "value"}, o);
            data.add(d);
        }

        List<String> dataFlags = Collections.singletonList(getNumericalFlag(num1));

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();
        renderOption.put("dataFlags", dataFlags);

        return JSONUtils.toJSONObject(
                new String[]{"data", "name", "_renderOption"},
                new Object[]{data, num1.getLabel(), renderOption});
    }
}
