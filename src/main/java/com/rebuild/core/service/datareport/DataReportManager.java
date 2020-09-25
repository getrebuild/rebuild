/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.RebuildConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据报表
 *
 * @author devezhao
 * @since 2019/8/13
 */
public class DataReportManager implements ConfigManager {

    public static final DataReportManager instance = new DataReportManager();

    private DataReportManager() {
    }

    /**
     * 获取报表列表
     *
     * @param entity
     * @return
     */
    public JSONArray getReports(Entity entity) {
        JSONArray list = new JSONArray();
        for (ConfigBean e : getReportsRaw(entity)) {
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
    public ConfigBean[] getReportsRaw(Entity entity) {
        final String cKey = "DataReportManager-" + entity.getName();
        ConfigBean[] cached = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cached != null) {
            return cached;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,isDisabled,templateFile from DataReportConfig where belongEntity = ?")
                .setParameter(1, entity.getName())
                .array();

        List<ConfigBean> list = new ArrayList<>();
        for (Object[] o : array) {
            ConfigBean e = new ConfigBean();
            e.set("id", o[0]);
            e.set("name", o[1]);
            e.set("disabled", o[2]);
            e.set("template", o[3]);
            list.add(e);
        }

        cached = list.toArray(new ConfigBean[0]);
        Application.getCommonsCache().putx(cKey, cached);
        return cached;
    }

    /**
     * @param entity
     * @param reportId
     * @return
     */
    public File getTemplateFile(Entity entity, ID reportId) {
        String template = null;
        for (ConfigBean e : getReportsRaw(entity)) {
            if (e.getID("id").equals(reportId)) {
                template = e.getString("template");
                break;
            }
        }

        if (template == null) {
            throw new ConfigurationException("No template of report found : " + reportId);
        }

        File file = RebuildConfiguration.getFileOfData(template);
        if (!file.exists()) {
            throw new ConfigurationException("File of template not extsts : " + file);
        }
        return file;
    }

    /**
     * @param reportId
     * @return
     * @see #getTemplateFile(Entity, ID) 性能好
     */
    @Deprecated
    public File getTemplateFile(ID reportId) {
        Object[] report = Application.createQueryNoFilter(
                "select belongEntity from DataReportConfig where configId = ?")
                .setParameter(1, reportId)
                .unique();
        if (report == null || !MetadataHelper.containsEntity((String) report[0])) {
            throw new ConfigurationException("No config of report found : " + reportId);
        }

        return getTemplateFile(MetadataHelper.getEntity((String) report[0]), reportId);
    }

    @Override
    public void clean(Object entity) {
        final String cKey = "DataReportManager-" + ((Entity) entity).getName();
        Application.getCommonsCache().evict(cKey);
    }
}
