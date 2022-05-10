/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.service.dashboard.charts.builtin.BuiltinChart;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.ArrayUtils;

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
        ConfigBean cb = (ConfigBean) Application.getCommonsCache().getx(ckey);
        if (cb != null) {
            return cb.clone();
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

        cb = new ConfigBean()
                .set("title", o[0])
                .set("type", o[1])
                .set("config", o[2] instanceof JSON ? o[2] : JSON.parse((String) o[2]))
                .set("createdBy", o[3]);
        Application.getCommonsCache().putx(ckey, cb);
        return cb.clone();
    }

    /**
     * 获取用户可用的图表列表
     *
     * @param user
     * @param specEntity [指定实体的]
     * @param onlySelf [自己的]
     * @return
     */
    public JSONArray getChartList(ID user, String[] specEntity, boolean onlySelf) {
        final String ckey = "Charts-ALL";

        Object[][] value = (Object[][]) Application.getCommonsCache().getx(ckey);
        if (value == null) {
            value = Application.createQueryNoFilter(
                    "select createdBy,belongEntity,config,chartId,title,chartType from ChartConfig")
                    .array();
            Application.getCommonsCache().putx(ckey, value);
        }

        JSONArray charts = new JSONArray();
        for (Object[] o : value) {
            ID createdBy = (ID) o[0];
            String belongEntity = (String) o[1];
            if (!MetadataHelper.containsEntity(belongEntity)) continue;

            // 过滤实体
            if (specEntity != null && !ArrayUtils.contains(specEntity, belongEntity)) continue;

            Entity entity = MetadataHelper.getEntity(belongEntity);

            // 权限不允许
            if (!Application.getPrivilegesManager().allowRead(user, entity.getEntityCode())) continue;

            boolean self = UserHelper.isSelf (user, createdBy);
            if (!self) {
                // 只要自己的
                if (onlySelf) continue;

                JSONObject config = JSONUtils.wellFormat((String) o[2]) ? JSON.parseObject((String) o[2]) : null;
                JSONObject chartOption = config == null ? null : config.getJSONObject("option");
                if (chartOption == null
                        || !chartOption.containsKey("shareChart") || !chartOption.getBoolean("shareChart")) {
                    continue;
                }
            }

            charts.add(JSONUtils.toJSONObject(
                    new String[] { "id", "title", "type", "entityLabel", "isManageable" },
                    new Object[] { o[3], o[4], o[5], EasyMetaFactory.getLabel(entity), self }));
        }
        return charts;
    }

    /**
     * 丰富图表数据 title, type
     *
     * @param charts
     * @param user [可选]
     */
    public void richingCharts(JSONArray charts, ID user) {
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

            if (user != null) {
                ID createdBy = e.getID("createdBy");
                ch.put("isManageable", UserHelper.isSelf(user, createdBy));
            }
        }
    }

    @Override
    public void clean(Object chartId) {
        final String ckey = "Charts-ALL";
        Application.getCommonsCache().evict(ckey);
        Application.getCommonsCache().evict("Chart-" + chartId);
    }
}
