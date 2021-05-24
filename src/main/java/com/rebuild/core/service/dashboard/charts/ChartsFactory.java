/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.dashboard.charts.builtin.ApprovalList;
import com.rebuild.core.service.dashboard.charts.builtin.BuiltinChart;
import com.rebuild.core.service.dashboard.charts.builtin.FeedsSchedule;
import com.rebuild.core.support.i18n.Language;

/**
 * @author devezhao
 * @since 12/15/2018
 */
public class ChartsFactory {

    /**
     * @param chartId
     * @return
     * @throws ChartsException
     */
    public static ChartData create(ID chartId) throws ChartsException {
        ConfigBean chart = ChartManager.instance.getChart(chartId);
        if (chart == null) {
            throw new ChartsException(Language.L("无效图表"));
        }

        JSONObject config = (JSONObject) chart.getJSON("config");
        config.put("chartOwning", chart.getID("createdBy"));
        return create(config, UserContextHolder.getUser());
    }

    /**
     * @param config
     * @param user
     * @return
     * @throws ChartsException
     */
    public static ChartData create(JSONObject config, ID user) throws ChartsException {
        String entityName = config.getString("entity");
        if (!MetadataHelper.containsEntity(entityName)) {
            throw new ChartsException(Language.L("源实体 [%s] 已不存在", entityName.toUpperCase()));
        }

        Entity entity = MetadataHelper.getEntity(entityName);
        if (user == null || !Application.getPrivilegesManager().allowRead(user, entity.getEntityCode())) {
            throw new DefinedException(Language.L("没有读取 %s 的权限", EasyMetaFactory.getLabel(entity)));
        }

        String type = config.getString("type");
        if ("INDEX".equalsIgnoreCase(type)) {
            return (ChartData) new IndexChart(config).setUser(user);
        } else if ("TABLE".equalsIgnoreCase(type)) {
            return (ChartData) new TableChart(config).setUser(user);
        } else if ("LINE".equalsIgnoreCase(type)) {
            return (ChartData) new LineChart(config).setUser(user);
        } else if ("BAR".equalsIgnoreCase(type)) {
            return (ChartData) new BarChart(config).setUser(user);
        } else if ("PIE".equalsIgnoreCase(type)) {
            return (ChartData) new PieChart(config).setUser(user);
        } else if ("FUNNEL".equalsIgnoreCase(type)) {
            return (ChartData) new FunnelChart(config).setUser(user);
        } else if ("TREEMAP".equalsIgnoreCase(type)) {
            return (ChartData) new TreemapChart(config).setUser(user);
        } else if ("RADAR".equalsIgnoreCase(type)) {
            return (ChartData) new RadarChart(config).setUser(user);
        } else if ("SCATTER".equalsIgnoreCase(type)) {
            return (ChartData) new ScatterChart(config).setUser(user);
        } else {
            for (BuiltinChart ch : getBuiltinCharts()) {
                if (ch.getChartType().equalsIgnoreCase(type)) {
                    return (ChartData) ((ChartData) ch).setUser(user);
                }
            }
        }
        throw new ChartsException("Unknown chart type : " + type);
    }

    /**
     * 获取内置图表
     *
     * @return
     */
    public static BuiltinChart[] getBuiltinCharts() {
        return new BuiltinChart[]{
                new ApprovalList(),
                new FeedsSchedule()
        };
    }
}
