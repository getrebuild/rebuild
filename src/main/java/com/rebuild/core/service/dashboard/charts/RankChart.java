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
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;

/**
 * 排行榜
 *
 * @author devezhao
 * @since 9/2/2026
 */
public class RankChart extends LineChart {

    protected RankChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        JSONObject res = (JSONObject) super.build();

        JSONArray xAxis = res.getJSONArray("xAxis");
        JSONArray yyyAxis = res.getJSONArray("yyyAxis");
        if (CollectionUtils.isEmpty(xAxis) || CollectionUtils.isEmpty(yyyAxis)) return res;

        JSONObject renderOption = res.getJSONObject("_renderOption");
        int pageSize = renderOption != null ? renderOption.getIntValue("pageSize") : 0;

        // 按第一个数值系列降序排序
        JSONObject firstSeries = yyyAxis.getJSONObject(0);
        JSONArray firstData = firstSeries.getJSONArray("data");
        int len = xAxis.size();
        Integer[] indices = new Integer[len];
        for (int i = 0; i < len; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> {
            double va = ObjectUtils.toDouble(EasyDecimal.clearFlaged(firstData.get(a)));
            double vb = ObjectUtils.toDouble(EasyDecimal.clearFlaged(firstData.get(b)));
            return Double.compare(vb, va);
        });

        // 重建排序后的数据（所有系列同步排序）
        JSONArray sortedXAxis = new JSONArray();
        for (int idx : indices) {
            sortedXAxis.add(xAxis.get(idx));
        }
        for (int s = 0; s < yyyAxis.size(); s++) {
            JSONObject series = yyyAxis.getJSONObject(s);
            JSONArray data = series.getJSONArray("data");
            JSONArray sortedData = new JSONArray();
            for (int idx : indices) {
                sortedData.add(data.get(idx));
            }
            series.put("data", sortedData);
        }

        // 限制条数
        if (pageSize > 0 && sortedXAxis.size() > pageSize) {
            JSONArray limitedXAxis = new JSONArray();
            for (int i = 0; i < pageSize; i++) {
                limitedXAxis.add(sortedXAxis.get(i));
            }
            sortedXAxis = limitedXAxis;

            for (int s = 0; s < yyyAxis.size(); s++) {
                JSONObject series = yyyAxis.getJSONObject(s);
                JSONArray data = series.getJSONArray("data");
                JSONArray limitedData = new JSONArray();
                for (int i = 0; i < pageSize; i++) {
                    limitedData.add(data.get(i));
                }
                series.put("data", limitedData);
            }
        }

        res.put("xAxis", sortedXAxis);

        return res;
    }
}
