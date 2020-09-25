/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSONObject;

/**
 * 柱状图
 *
 * @author devezhao
 * @since 12/15/2018
 */
public class BarChart extends LineChart {

    protected BarChart(JSONObject config) {
        super(config);
    }
}
