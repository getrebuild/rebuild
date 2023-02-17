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
 * 漏斗图
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class FunnelChart extends ChartData {

    protected FunnelChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        JSONArray dataJson = new JSONArray();
        List<String> dataFlags = new ArrayList<>();

        if (nums.length > 1) {
            Object[] dataRaw = createQuery(buildSql(nums)).unique();
            for (int i = 0; i < nums.length; i++) {
                JSONObject d = JSONUtils.toJSONObject(
                        new String[]{"name", "value"},
                        new Object[]{nums[i].getLabel(), wrapAxisValue(nums[i], dataRaw[i])});
                dataJson.add(d);
                dataFlags.add(getNumericalFlag(nums[i]));
            }

        } else if (nums.length == 1 && dims.length >= 1) {
            Dimension dim1 = dims[0];
            Object[][] dataRaw = createQuery(buildSql(dim1, nums[0])).array();
            final String valueFlag = getNumericalFlag(nums[0]);
            for (Object[] o : dataRaw) {
                JSONObject d = JSONUtils.toJSONObject(
                        new String[]{"name", "value"},
                        new Object[]{o[0] = wrapAxisValue(dim1, o[0]), wrapAxisValue(nums[0], o[1])});
                dataJson.add(d);
                dataFlags.add(valueFlag);
            }

            if (dim1.getFormatSort() != FormatSort.NONE) {
                dataJson.sort((a, b) -> {
                    String aName = ((JSONObject) a).getString("name");
                    String bName = ((JSONObject) b).getString("name");
                    if (dim1.getFormatSort() == FormatSort.ASC) {
                        return aName.compareTo(bName);
                    } else {
                        return bName.compareTo(aName);
                    }
                });
            }
        }

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();
        renderOption.put("dataFlags", dataFlags);

        JSONObject ret = JSONUtils.toJSONObject(
                new String[]{"data", "_renderOption"},
                new Object[]{dataJson, renderOption});
        if (nums.length >= 1 && dims.length >= 1) {
            ret.put("xLabel", nums[0].getLabel());
        }
        return ret;
    }
}
