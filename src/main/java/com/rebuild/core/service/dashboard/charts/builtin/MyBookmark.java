package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

/**
 * 我的常用
 *
 * @author devezhao
 * @since 2025/12/3
 */
public class MyBookmark extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000008");

    public MyBookmark() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("我的常用");
    }

    @Override
    public JSON build() {
        String ckey = "MyBookmark:" + getUser();
        String s = RebuildConfiguration.getCustomValue(ckey);
        return s == null ? JSONUtils.EMPTY_ARRAY : JSON.parseArray(s);
    }
}
