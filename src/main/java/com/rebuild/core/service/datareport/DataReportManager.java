/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigManager;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

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

    private DataReportManager() {}

    public static final int TYPE_RECORD = 1;
    public static final int TYPE_LIST = 2;
    public static final int TYPE_HTML5 = 3;
    public static final int TYPE_WORD = 4;

    /**
     * 获取可用报表
     *
     * @param entity
     * @param type 指定类型
     * @param recordId
     * @return
     */
    public JSONArray getReportTemplates(Entity entity, int type, ID recordId) {
        JSONArray alist = new JSONArray();
        for (ConfigBean e : getReportsRaw(entity)) {
            if (e.getBoolean("disabled")) continue;

            boolean can;
            int aType = e.getInteger("type");
            if (type == DataReportManager.TYPE_LIST) {
                can = aType == type;
            } else {
                can = aType == DataReportManager.TYPE_RECORD
                        || aType == DataReportManager.TYPE_WORD || aType == DataReportManager.TYPE_HTML5;
            }

            if (can) {
                // v3.7
                String useFilter = e.getString("useFilter");
                if (JSONUtils.wellFormat(useFilter) && recordId != null) {
                    can = QueryHelper.isMatchAdvFilter(recordId, JSON.parseObject(useFilter));
                }
            }

            if (can) alist.add(e.toJSON("id", "name", "outputType"));
        }
        return alist;
    }

    /**
     * 获取报表列表（含禁用）
     *
     * @param entity
     * @return
     */
    public ConfigBean[] getReportsRaw(Entity entity) {
        final String cKey = "DataReportManager38-" + entity.getName();
        ConfigBean[] cached = (ConfigBean[]) Application.getCommonsCache().getx(cKey);
        if (cached != null) return cached;

        Object[][] array = Application.createQueryNoFilter(
                "select configId,name,isDisabled,templateFile,templateType,extraDefinition,templateContent from DataReportConfig where belongEntity = ?")
                .setParameter(1, entity.getName())
                .array();

        List<ConfigBean> alist = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject extra = o[5] == null ? JSONUtils.EMPTY_OBJECT : JSON.parseObject((String) o[5]);
            String outputType = StringUtils.defaultIfBlank(extra.getString("outputType"), "excel");
            int templateVersion = extra.containsKey("templateVersion") ? extra.getInteger("templateVersion") : 2;
            String useFilter = extra.getString("useFilter");

            int type = ObjectUtils.toInt(o[4], TYPE_RECORD);
            if (type == TYPE_WORD && outputType.contains("excel")) outputType += ",word";
            else if (type == TYPE_HTML5) outputType = "html5";

            ConfigBean cb = new ConfigBean()
                    .set("id", o[0])
                    .set("name", o[1])
                    .set("disabled", o[2])
                    .set("template", o[3])
                    .set("type", type)
                    .set("outputType", outputType)
                    .set("templateVersion", templateVersion)
                    .set("useFilter", useFilter)
                    .set("templateContent", o[6])
                    .set("entity", entity.getName());
            alist.add(cb);
        }

        cached = alist.toArray(new ConfigBean[0]);
        Application.getCommonsCache().putx(cKey, cached);
        return cached;
    }

    /**
     * @param reportId
     * @return
     * @see #getReportsRaw(Entity)
     */
    private ConfigBean getReportRaw(ID reportId, Entity entity) {
        if (entity == null) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(reportId, "belongEntity");
            if (o == null || !MetadataHelper.containsEntity((String) o[0])) {
                throw new ConfigurationException("No config of report found : " + reportId);
            }
            entity = MetadataHelper.getEntity((String) o[0]);
        }

        ConfigBean[] cbs = getReportsRaw(entity);
        for (ConfigBean cb : cbs) {
            if (reportId.equals(cb.getID("id"))) return cb;
        }
        throw new ConfigurationException("No config of report found : " + reportId);
    }

    /**
     * 获取报表配置
     *
     * @param reportId
     * @return
     */
    public ConfigBean getReportRaw(ID reportId) {
        return getReportRaw(reportId, null);
    }

    /**
     * @param reportId
     * @param entity
     * @return
     */
    public TemplateFile buildTemplateFile(ID reportId, Entity entity) {
        final ConfigBean conf = getReportRaw(reportId, entity);
        String templateFile = conf.getString("template");
        String templateContent = conf.getString("templateContent");
        int type = conf.getInteger("type");
        boolean isV33 = conf.getInteger("templateVersion") == 3;
        if (type == TYPE_HTML5) {
            if (templateContent == null) templateContent = "";
        } else {
            templateContent = null;
        }

        if (entity == null) entity = MetadataHelper.getEntity(conf.getString("entity"));

        // v35 HTML5
        if (templateContent != null) {
            return new TemplateFile(templateContent, entity, reportId);
        }

        if (templateFile == null) {
            throw new ConfigurationException("No template of report found : " + reportId);
        }

        File file = RebuildConfiguration.getFileOfData(templateFile);
        if (!file.exists()) {
            throw new ConfigurationException("File of template not extsts : " + file);
        }

        return new TemplateFile(file, entity, type, isV33, reportId);
    }

    /**
     * @param reportId
     * @return
     * @see #buildTemplateFile(ID, Entity)
     */
    public TemplateFile buildTemplateFile(ID reportId) {
        return buildTemplateFile(reportId, null);
    }

    @Override
    public void clean(Object entity) {
        final String cKey = "DataReportManager38-" + ((Entity) entity).getName();
        Application.getCommonsCache().evict(cKey);
    }

    // --

    /**
     * 获取报表名称
     *
     * @param reportId
     * @param idOrEntity
     * @param fileName
     * @return
     */
    public static String getPrettyReportName(ID reportId, Object idOrEntity, String fileName) {
        final Entity be = idOrEntity instanceof ID
                ? MetadataHelper.getEntity(((ID) idOrEntity).getEntityCode())
                : MetadataHelper.getEntity((String) idOrEntity);

        ConfigBean conf = DataReportManager.instance.getReportRaw(reportId, be);
        String name = conf.getString("name");
        if (ContentWithFieldVars.matchsVars(name).isEmpty()) {
            name = String.format("%s-%s", name, CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()));
        } else if (idOrEntity instanceof ID) {
            name = ContentWithFieldVars.replaceWithRecord(name, (ID) idOrEntity);
        }
        // Suffix
        if (fileName.endsWith(".pdf")) name += ".pdf";
        else if (fileName.endsWith(".docx")) name += ".docx";
        else if (fileName.endsWith(".doc")) name += ".doc";
        else if (fileName.endsWith(".xlsx")) name += ".xlsx";
        else if (fileName.endsWith(".xls")) name += ".xls";
        else if (fileName.endsWith(".csv")) name += ".csv";
        else if (fileName.endsWith(".zip")) name += ".zip";

        return StringUtils.defaultIfBlank(name, "UNTITLE");
    }
}
