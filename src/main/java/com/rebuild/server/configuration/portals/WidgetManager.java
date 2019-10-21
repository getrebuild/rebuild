/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.configuration.ConfigEntry;

/**
 * 页面部件
 *
 * @author devezhao
 * @since 2019/10/18
 */
public class WidgetManager extends ShareToManager<ID> {

    public static final WidgetManager instance = new WidgetManager();
    private WidgetManager() { }

    public static final String TYPE_DATALIST = BaseLayoutManager.TYPE_DATALIST;

    @Override
    protected String getConfigEntity() {
        return "WidgetConfig";
    }

    /**
     * 列表页-图表
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigEntry getDataListChart(ID user, String entity) {
        ID detected = detectUseConfig(user, entity, TYPE_DATALIST);
        if (detected == null) {
            return null;
        }

        Object[][] canUses = getUsesConfig(user, entity, TYPE_DATALIST);
        for (Object[] c : canUses) {
            if (!c[0].equals(detected)) continue;

            JSONArray charts = JSON.parseArray((String) c[3]);
            ChartManager.instance.richingCharts(charts);
            return new ConfigEntry()
                    .set("id", c[0])
                    .set("config", charts);
        }
        return null;
    }

    @Override
    public void clean(ID cacheKey) {
        cleanWithBelongEntity(cacheKey, true);
    }
}
