/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.dashboard.DashboardManager;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 查看可用的图表、仪表盘
 *
 * @author devezhao
 * @since 2026/11/4
 */
public class ListCharts implements Tool {

    private static final String TYPE_CHART = "chart";
    private static final String TYPE_DASH = "dash";

    private static final String TYPE_ALL = "chart、dash";

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String listType = args.getString("listType");
        if (StringUtils.isBlank(listType)) {
            throw new KnownToolException("listType 不能为空，可用值 " + TYPE_ALL);
        }

        switch (listType) {
            case TYPE_CHART:
                return listCharts(args);
            case TYPE_DASH:
                return listDashs();
            default:
                throw new KnownToolException("未知的查询类型 : " + listType + "，可用值 " + TYPE_ALL);
        }
    }

    /**
     * @param args
     * @return
     */
    private JSONObject listCharts(JSONObject args) {
        final ID user = UserContextHolder.getUser();

        String[] specEntity = null;
        String entityIdent = args.getString("entity");
        if (StringUtils.isNotBlank(entityIdent)) {
            Entity entity = ToolHelper.resolveEntity(entityIdent);
            if (entity == null) {
                throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
            }
            specEntity = MetadataHelper.resolveEntityFamily(entity);
        }

        boolean onlySelf = args.getBooleanValue("myself");

        JSONArray charts = ChartManager.instance.getChartList(user, specEntity, onlySelf);

        return JSONUtils.toJSONObject(
                new String[]{"status", "total", "charts"},
                new Object[]{"ok", charts.size(), charts});
    }

    /**
     * @return
     */
    private JSONObject listDashs() {
        final ID user = UserContextHolder.getUser();
        boolean isAdmin = UserHelper.isAdmin(user);

        JSONArray available = (JSONArray) DashboardManager.instance.getAvailable(user, false);
        if (available == null) available = new JSONArray();

        JSONArray dashboards = new JSONArray();
        for (int i = 0; i < available.size(); i++) {
            JSONArray item = available.getJSONArray(i);
            // [configId, shareTo, isSelf, charts, title]
            String id = item.getString(0);
            boolean isSelf = item.getBooleanValue(2);
            JSONArray charts = item.getJSONArray(3);
            String title = item.getString(4);

            JSONObject dash = new JSONObject(true);
            dash.put("id", id);
            dash.put("title", title);
            dash.put("editable", isAdmin || isSelf);
            dash.put("chartCount", charts != null ? charts.size() : 0);
            dashboards.add(dash);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "total", "dashboards"},
                new Object[]{"ok", dashboards.size(), dashboards});
    }

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
