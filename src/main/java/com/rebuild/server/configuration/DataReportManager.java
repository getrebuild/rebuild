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

package com.rebuild.server.configuration;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.helper.SysConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据报表
 *
 * @author devezhao
 * @since 2019/8/13
 */
public class DataReportManager implements ConfigManager<Entity> {

    public static final DataReportManager instance = new DataReportManager();
    private DataReportManager() {}

    /**
     * 获取报表列表
     *
     * @param entity
     * @return
     */
    public JSONArray getReports(Entity entity) {
        JSONArray list = new JSONArray();
        for (ConfigEntry e : getReportsRaw(entity)) {
            if (!e.getBoolean("disabled")) {
                list.add(e.toJSON());
            }
        }
        return list;
    }

    /**
     * 获取报表列表（含禁用）
     *
     * @param entity
     * @return
     */
    public ConfigEntry[] getReportsRaw(Entity entity) {
        final String cKey = "DataReportManager-" + entity.getName();
        ConfigEntry[] reports = (ConfigEntry[]) Application.getCommonCache().getx(cKey);
        if (reports != null) {
            return reports;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,isDisabled,templateFile from DataReportConfig where belongEntity = ?")
                .setParameter(1, entity.getName())
                .array();

        List<ConfigEntry> list = new ArrayList<>();
        for (Object[] o : array) {
            ConfigEntry e = new ConfigEntry();
            e.set("id", o[0]);
            e.set("name", o[1]);
            e.set("disabled", o[2]);
            e.set("template", o[3]);
            list.add(e);
        }

        reports = list.toArray(new ConfigEntry[list.size()]);
        Application.getCommonCache().putx(cKey, reports);
        return reports;
    }

    /**
     * @param entity
     * @param reportId
     * @return
     */
    public File getTemplate(Entity entity, ID reportId) {
        String template = null;
        for (ConfigEntry e : getReportsRaw(entity)) {
            if (e.getID("id").equals(reportId)) {
                template = e.getString("template");
                break;
            }
        }

        if (template == null) {
            throw new ConfigurationException("No template of report found : " + reportId);
        }

        File file = SysConfiguration.getFileOfData(template);
        if (!file.exists()) {
            throw new ConfigurationException("Template file not extsts : " + file);
        }
        return file;
    }

    @Override
    public void clean(Entity cacheKey) {
        final String cKey = "DataReportManager-" + cacheKey.getName();
        Application.getCommonCache().evict(cKey);
    }
}
