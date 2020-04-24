/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * TODO
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
        return null;
    }
}
