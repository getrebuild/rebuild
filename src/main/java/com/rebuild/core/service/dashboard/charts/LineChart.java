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

import java.util.ArrayList;
import java.util.List;

/**
 * 曲线图
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class LineChart extends ChartData {

    protected LineChart(JSONObject config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Dimension dim1 = dims[0];
        Object[][] dataRaw = createQuery(buildSql(dim1, nums)).array();

        List<String> dimAxis = new ArrayList<>();
        Object[] numsAxis = new Object[nums.length];
        for (Object[] o : dataRaw) {
            dimAxis.add(wrapAxisValue(dim1, o[0]));

            for (int i = 0; i < nums.length; i++) {
                List<String> numAxis = (List<String>) numsAxis[i];
                if (numAxis == null) {
                    numAxis = new ArrayList<>();
                    numsAxis[i] = numAxis;
                }
                numAxis.add(wrapAxisValue(nums[i], o[i + 1]));
            }
        }

        JSONArray yyyAxis = new JSONArray();
        JSONArray yyyAxisFlags = new JSONArray();
        for (int i = 0; i < nums.length; i++) {
            Numerical axis = nums[i];
            List<String> data = (List<String>) numsAxis[i];

            JSONObject map = new JSONObject();
            map.put("name", axis.getLabel());
            map.put("data", data);
            yyyAxis.add(map);
            yyyAxisFlags.add(getValueFlag(axis));
        }

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();
        renderOption.put("yyyAxisFlags", yyyAxisFlags);

        return JSONUtils.toJSONObject(
                new String[]{"xAxis", "yyyAxis", "_renderOption"},
                new Object[]{JSON.toJSON(dimAxis), JSON.toJSON(yyyAxis), renderOption});
    }
}
