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
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;

/**
 * TODO
 *
 * @author devezhao
 * @since 2019/10/18
 */
public class WidgetManager extends SharableManager<ID> {

    public static final WidgetManager instance = new WidgetManager();
    private WidgetManager() { }

    /**
     * 列表页图表
     *
     * @param user
     * @param entity
     * @return
     */
    public ConfigEntry getDataListChart(ID user, String entity) {
        ID detected = detectUseConfig(user, "WidgetConfig", entity, BaseLayoutManager.TYPE_DATALIST);
        if (detected == null) {
            return null;
        }

        String ckey = "DataListChart-" + detected;
        ConfigEntry config = (ConfigEntry) Application.getCommonCache().getx(ckey);
        if (config == null) {
            Object[] o = Application.createQueryNoFilter(
                    "select config from WidgetConfig where configId = ?")
                    .setParameter(1, detected)
                    .unique();
            config = new ConfigEntry();
            config.set("id", detected);
            config.set("config", JSON.parseArray((String) o[0]));
            Application.getCommonCache().putx(ckey, config);
        }

        JSONArray charts = (JSONArray) config.getJSON("config");
        ChartManager.instance.richingCharts(charts);
        config.set("config", charts);
        return config;
    }

    @Override
    public void clean(ID cacheKey) {
        Application.getCommonCache().evict("DataListChart-" + cacheKey);

        Object[] c = Application.createQueryNoFilter(
                "select belongEntity,applyType from WidgetConfig where configId = ?")
                .setParameter(1, cacheKey)
                .unique();
        if (c != null) {
            String ck = String.format("%s-%s-%s", "WidgetConfig", c[0], c[1]);
            Application.getCommonCache().evict(ck);
        }
    }
}
