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

        JSONArray dataArray = new JSONArray();
        List<String> dataFlags = new ArrayList<>();

        // 0DIM + 2~9NUM
        if (nums.length > 1) {
            for (Numerical num : nums) {
                Object[] dataRaw = createQuery(buildSql(num, true)).unique();
                dataRaw = this.calcFormula43(dataRaw, num);

                JSONObject d = JSONUtils.toJSONObject(
                        new String[]{"name", "value"},
                        new Object[]{num.getLabel(), wrapAxisValue(num, dataRaw[0])});
                dataArray.add(d);
                dataFlags.add(getNumericalFlag(num));
            }
        }
        // 1DIM + 1NUM
        else if (nums.length == 1 && dims.length >= 1) {
            Dimension dim1 = dims[0];  // 多余的不要
            Numerical num1 = nums[0];
            Object[][] dataRaw = createQuery(buildSql(dim1, num1, true)).array();
            this.calcFormula43(dataRaw, nums);

            final String valueFlag = getNumericalFlag(num1);
            for (Object[] o : dataRaw) {
                JSONObject d = JSONUtils.toJSONObject(
                        new String[]{"name", "value"},
                        new Object[]{o[0] = wrapAxisValue(dim1, o[0]), wrapAxisValue(num1, o[1])});
                dataArray.add(d);
                dataFlags.add(valueFlag);
            }

            if (dim1.getFormatSort() != FormatSort.NONE) {
                dataArray.sort((a, b) -> {
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

        // 转化率
        if (renderOption.getBooleanValue("showCvr")) {
            Double last = null;
            for (Object o : dataArray) {
                JSONObject d = (JSONObject) o;
                String value = EasyDecimal.clearFlaged(d.getString("value"));
                double n = ObjectUtils.toDouble(value);
                if (last == null) {
                    d.put("cvr", false);
                } else {
                    d.put("cvr", ObjectUtils.round(n * 100 / last, 2));
                }
                last = n;
            }
        }

        JSONObject ret = JSONUtils.toJSONObject(
                new String[]{"data", "_renderOption"},
                new Object[]{dataArray, renderOption});
        if (nums.length >= 1 && dims.length >= 1) {
            ret.put("xLabel", nums[0].getLabel());
        }
        return ret;
    }
}
