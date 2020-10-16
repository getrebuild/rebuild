/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.data;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.TemplateExtractor;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Excel 报表
 *
 * @author devezhao
 * @since 2019/8/13
 */
@Controller
@RequestMapping("/admin/data/")
public class ReportTemplateController extends BaseController {

    @RequestMapping("/report-templates")
    public ModelAndView page() {
        return createModelAndView("/admin/data/report-templates");
    }

    @RequestMapping("/report-templates/list")
    public void reportList(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn from DataReportConfig" +
                " where (1=1) and (2=2)" +
                " order by name, modifiedOn desc";

        Object[][] array = queryListOfConfig(sql, entity, q);
        writeSuccess(response, array);
    }

    @RequestMapping("/report-templates/check-template")
    public void checkTemplate(HttpServletRequest request, HttpServletResponse response) {
        String file = getParameterNotNull(request, "file");
        String entity = getParameterNotNull(request, "entity");

        File template = RebuildConfiguration.getFileOfData(file);
        Entity entityMeta = MetadataHelper.getEntity(entity);

        Map<String, String> vars = new TemplateExtractor(template, true).transformVars(entityMeta);
        if (vars.isEmpty()) {
            writeFailure(response, "无效模板文件 (未找到任何字段)");
            return;
        }

        Set<String> invalidVars = new HashSet<>();
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getValue() == null) {
                invalidVars.add(e.getKey());
            }
        }

        if (invalidVars.size() >= vars.size()) {
            writeFailure(response, "无效模板文件 (未找到有效字段)");
            return;
        }

        JSON ret = JSONUtils.toJSONObject("invalidVars", invalidVars);
        writeSuccess(response, ret);
    }

    @RequestMapping("/report-templates/preview")
    public void preview(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID reportId = getIdParameterNotNull(request, "id");
        Object[] report = Application.createQueryNoFilter(
                "select belongEntity from DataReportConfig where configId = ?")
                .setParameter(1, reportId)
                .unique();
        Entity entity = MetadataHelper.getEntity((String) report[0]);

        String sql = String.format("select %s from %s order by modifiedOn desc",
                entity.getPrimaryField().getName(), entity.getName());
        Object[] random = Application.createQueryNoFilter(sql).unique();
        if (random == null) {
            response.sendError(400, "无法预览。未找到可供预览的记录");
            return;
        }

        File file;
        try {
            File template = DataReportManager.instance.getTemplateFile(entity, reportId);
            file = new EasyExcelGenerator(template, (ID) random[0]).generate();
        } catch (ConfigurationException ex) {
            response.sendError(400, "无法预览。报表模板文件不存在");
            return;
        }

        FileDownloader.setDownloadHeaders(request, response, file.getName());
        FileDownloader.writeLocalFile(file, response);
    }

    // --

    /**
     * 查询配置列表
     *
     * @param sql
     * @param belongEntity
     * @param q
     */
    public static Object[][] queryListOfConfig(String sql, String belongEntity, String q) {
        if (StringUtils.isNotBlank(belongEntity) && !"$ALL$".equalsIgnoreCase(belongEntity)) {
            sql = sql.replace("(1=1)", "belongEntity = '" + StringEscapeUtils.escapeSql(belongEntity) + "'");
        }
        if (StringUtils.isNotBlank(q)) {
            sql = sql.replace("(2=2)", "name like '%" + StringEscapeUtils.escapeSql(q) + "%'");
        }

        Object[][] array = Application.createQuery(sql).setLimit(500).array();
        for (Object[] o : array) {
            o[2] = EasyMeta.getLabel(MetadataHelper.getEntity((String) o[2]));
            o[5] = CalendarUtils.getUTCDateTimeFormat().format(o[5]);
        }
        return array;
    }
}
