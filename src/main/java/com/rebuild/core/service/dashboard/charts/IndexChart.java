/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

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
        dataRaw = applyCalcFormula(dataRaw, num);

        JSONObject index = JSONUtils.toJSONObject(
                new String[]{"data", "label", "dataFlag"},
                new Object[]{wrapAxisValue(num, dataRaw[0]), num.getLabel(), getNumericalFlag(num)});

        // 对比
        if (nums.length > 1) {
            num = nums[1];
            dataRaw = createQuery(buildSql(num, true)).unique();
            dataRaw = applyCalcFormula(dataRaw, num);

            index.put("data2", wrapAxisValue(num, dataRaw[0]));
            index.put("label2", num.getLabel());
            index.put("dataFlag2", getNumericalFlag(num));
        }

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();

        return JSONUtils.toJSONObject(
                new String[]{"index", "_renderOption"},
                new Object[]{index, renderOption});
    }

    // 应用计算字段
    private Object[] applyCalcFormula(Object[] dataRaw, Numerical num) {
        if (StringUtils.isBlank(num.getFormatFormula())) return dataRaw;

        Object[][] dataRawArray = new Object[][]{dataRaw};
        Numerical[] nums = new Numerical[]{num};

        this.calcFormula43(dataRawArray, nums);
        return dataRawArray[0];
    }

}
