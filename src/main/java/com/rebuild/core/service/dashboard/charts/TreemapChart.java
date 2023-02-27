/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.commons.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

/**
 * 树图
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class TreemapChart extends ChartData {

    protected TreemapChart(JSONObject config) {
        super(config);
    }

    @Override
    public JSON build() {
        Dimension[] dims = getDimensions();
        Numerical[] nums = getNumericals();

        Numerical num1 = nums[0];
        Object[][] dataRaw = createQuery(buildSql(dims, num1)).array();

        int lastIndex = dataRaw.length > 0 ? dataRaw[0].length - 1 : 0;
        double xAmount = 0d;
        for (Object[] o : dataRaw) {
            double v = ObjectUtils.toDouble(o[lastIndex]);
            o[lastIndex] = v;
            xAmount += v;

            for (int j = 0; j < o.length - 1; j++) {
                o[j] = wrapAxisValue(dims[j], o[j]);
            }
        }

        TreeBuilder builder = new TreeBuilder(dataRaw, this);

        JSONObject renderOption = config.getJSONObject("option");
        if (renderOption == null) renderOption = new JSONObject();
        renderOption.put("dataFlags", new String[] { getNumericalFlag(num1) });

        return JSONUtils.toJSONObject(
                new String[]{"data", "xLabel", "xAmount", "_renderOption"},
                new Object[]{builder.toJSON(), num1.getLabel(), xAmount, renderOption});
    }

    @Override
    public Numerical[] getNumericals() {
        Numerical[] nums = super.getNumericals();
        if (nums.length == 0) {
            Numerical d = new Numerical(
                    getSourceEntity().getPrimaryField(), FormatSort.NONE, FormatCalc.COUNT, null, 0, null);
            return new Numerical[]{d};
        }
        return nums;
    }
}
