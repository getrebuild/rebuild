/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 雷达图
 *
 * @author ZHAO
 * @since 2020/4/25
 */
public class RadarChart extends ChartData {

    protected RadarChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Dimension dim1 = dims[0];
        Object[][] dataRaw = createQuery(buildSql(dim1, nums)).array();

        JSONArray indicator = new JSONArray();

        Map<Numerical, Object[]> seriesRotate = new LinkedHashMap<>();
        List<String> dataFlags = new ArrayList<>();
        for (Numerical n : nums) {
            seriesRotate.put(n, new Object[dataRaw.length]);
            dataFlags.add(getNumericalFlag(n));
        }

        for (int i = 0; i < dataRaw.length; i++) {
            Object[] item = dataRaw[i];

            indicator.add(JSONUtils.toJSONObject(
                    new String[]{"name", "max"},
                    new Object[]{wrapAxisValue(dim1, item[0]), calcMax(item)}));

            for (int j = 0; j < nums.length; j++) {
                Object[] data = seriesRotate.get(nums[j]);
                data[i] = wrapAxisValue(nums[j], item[j + 1]);
            }
        }

        JSONArray series = new JSONArray();
        for (Map.Entry<Numerical, Object[]> e : seriesRotate.entrySet()) {
            series.add(JSONUtils.toJSONObject(
                    new String[]{"name", "value"},
                    new Object[]{e.getKey().getLabel(), e.getValue()}));
        }

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();
        renderOption.put("dataFlags", dataFlags);

        return JSONUtils.toJSONObject(
                new String[]{"indicator", "series", "_renderOption"},
                new Object[]{indicator, series, renderOption});
    }

    private long calcMax(Object[] items) {
        long max = 0;
        for (int i = 1; i < items.length; i++) {
            long value = ObjectUtils.toLong(items[i]);
            if (value > max) {
                max = value;
            }
        }
        return (long) (max * 1.2d) + 1;
    }
}
