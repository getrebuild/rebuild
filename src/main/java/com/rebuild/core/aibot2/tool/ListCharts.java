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
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author devezhao
 * @since 2026/11/4
 */
public class ListCharts implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);
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

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
