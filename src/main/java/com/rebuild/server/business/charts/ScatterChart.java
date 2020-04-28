/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

/**
 * 散点图
 *
 * @author ZHAO
 * @since 2020/4/25
 */
public class ScatterChart extends ChartData {

    protected ScatterChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        JSONArray series = new JSONArray();

        if (dims.length == 0) {
            Object[][] dataRaw = createQuery(buildSql(nums)).array();
            for (Object[] item : dataRaw) {
                for (int i = 0; i < item.length; i++) {
                    item[i] = wrapAxisValue(nums[i], item[i]);
                }
            }

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "data" },
                    new Object[] { dataRaw });
            series.add(item);
        }
        else {
            for (Dimension dim : dims) {
                Object[][] dataRaw = createQuery(buildSql(dim, nums)).array();
                for (Object[] item : dataRaw) {
                    String label = wrapAxisValue(dim, item[0]);
                    for (int i = 1; i < item.length; i++) {
                        item[i - 1] = wrapAxisValue(nums[i - 1], item[i]);
                    }
                    item[item.length - 1] = label;
                }

                JSONObject item = JSONUtils.toJSONObject(
                        new String[] { "data", "name" },
                        new Object[] { dataRaw, dim.getLabel() });
                series.add(item);
            }
        }

        String[] dataLabel = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            dataLabel[i] = nums[i].getLabel();
        }

        return JSONUtils.toJSONObject(
                new String[] { "series", "dataLabel", "_renderOption" },
                new Object[] { series, dataLabel, config.getJSONObject("option") });
    }
}
