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
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 柏拉图/帕累托图
 *
 * @author devezhao
 * @since 2025/9/22
 */
public class ParetoChart extends Bar3Chart {

    protected ParetoChart(JSONObject config) {
        super(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSON build() {
        JSONObject res = (JSONObject) super.build();
        // 无数据
        if (CollectionUtils.isEmpty(res.getJSONArray("xAxis"))) return res;

        // 累计增长
        JSONArray yyyAxis = res.getJSONArray("yyyAxis");
        JSONObject precentAxis = (JSONObject) yyyAxis.get(1);
        List<Object> precentAxisData = (List<Object>) precentAxis.get("data");
        double total = 0;
        for (Object o : precentAxisData) {
            total += ObjectUtils.toDouble(EasyDecimal.clearFlaged(o));
        }

        List<Object> precentAxisDataNew = new ArrayList<>();
        double dd = 0;
        for (Object o : precentAxisData) {
            dd += ObjectUtils.toDouble(EasyDecimal.clearFlaged(o));
            if (Double.compare(dd, 0.0) == 0) {
                precentAxisDataNew.add(0);
            } else {
                double p = dd / total * 100;
                precentAxisDataNew.add(wrapAxisValue(getNumericals()[1], p));
            }
        }
        precentAxis.put("data", precentAxisDataNew);

        JSONObject _renderOption = res.getJSONObject("_renderOption");
        // 多轴显示
        _renderOption.put("showMutliYAxis", true);
        // %
        List<Object> dataFlags = (List<Object>) _renderOption.get("dataFlags");
        dataFlags.set(1, "%");

        return res;
    }

    @Override
    public Numerical[] getNumericals() {
        Numerical[] ns = super.getNumericals();
        Numerical ns0 = ns[0];
        // 固定排序
        Numerical n1 = new Numerical(ns0.getField(), FormatSort.DESC, ns0.getFormatCalc(),
                ns0.getLabel(), ns0.getScale(), ns0.getUnit(), ns0.getFilter(), ns0.getParentField());
        // 累计占比
        Numerical n2 = new Numerical(ns0.getField(), FormatSort.DESC, ns0.getFormatCalc(),
                Language.L("累计占比"), 2, 0, ns0.getFilter(), ns0.getParentField());
        return new Numerical[]{n1, n2};
    }
}
