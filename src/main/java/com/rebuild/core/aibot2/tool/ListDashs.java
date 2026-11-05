/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.dashboard.DashboardManager;
import com.rebuild.utils.JSONUtils;

/**
 * @author devezhao
 * @since 2026/11/4
 */
public class ListDashs implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
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
