/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用数据列表
 *
 * @author devezhao
 * @since 2023/1/6
 */
public class DataList extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000004");

    public DataList() {
        super(null);
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("数据列表");
    }

    @Override
    public JSON build() {
        List<Object> data = new ArrayList<>();

        return JSONUtils.toJSONObject(
                new String[] { "total", "data" }, new Object[] { 0, data });
    }
}
