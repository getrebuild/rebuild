/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.service.dashboard.charts.builtin.BuiltinChart;

import java.util.Iterator;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/06/04
 */
public class ChartManager implements ConfigManager {

    public static final ChartManager instance = new ChartManager();

    private ChartManager() {
    }

    /**
     * @param chartid
     * @return
     */
    public ConfigBean getChart(ID chartid) {
        final String ckey = "Chart-" + chartid;
        ConfigBean entry = (ConfigBean) Application.getCommonsCache().getx(ckey);
        if (entry != null) {
            return entry.clone();
        }

        Object[] o = Application.createQueryNoFilter(
                "select title,chartType,config,createdBy from ChartConfig where chartId = ?")
                .setParameter(1, chartid)
                .unique();
        if (o == null) {
            for (BuiltinChart ch : ChartsFactory.getBuiltinCharts()) {
                if (chartid.equals(ch.getChartId())) {
                    o = new Object[]{ch.getChartTitle(), ch.getChartType(), ch.getChartConfig(), UserService.SYSTEM_USER};
                }
            }

            if (o == null) {
                return null;
            }
        }

        entry = new ConfigBean()
                .set("title", o[0])
                .set("type", o[1])
                .set("config", o[2] instanceof JSON ? (JSON) o[2] : JSON.parse((String) o[2]))
                .set("createdBy", o[3]);
        Application.getCommonsCache().putx(ckey, entry);
        return entry.clone();
    }

    /**
     * 丰富图表数据 title, type
     *
     * @param charts
     */
    public void richingCharts(JSONArray charts) {
        for (Iterator<Object> iter = charts.iterator(); iter.hasNext(); ) {
            JSONObject ch = (JSONObject) iter.next();
            ID chartid = ID.valueOf(ch.getString("chart"));
            ConfigBean e = getChart(chartid);
            if (e == null) {
                iter.remove();
                continue;
            }

            ch.put("title", e.getString("title"));
            ch.put("type", e.getString("type"));
        }
    }

    @Override
    public void clean(Object chartId) {
        Application.getCommonsCache().evict("Chart-" + chartId);
    }
}
