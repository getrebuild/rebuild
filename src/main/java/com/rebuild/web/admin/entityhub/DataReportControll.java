/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.business.datareport.EasyExcelGenerator;
import com.rebuild.server.business.datareport.TemplateExtractor;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.common.FileDownloader;
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
@RequestMapping("/admin/datas/")
public class DataReportControll extends BasePageControll {

    @RequestMapping("/data-reports")
    public ModelAndView pageDataReports() {
        return createModelAndView("/admin/entityhub/data-reports.jsp");
    }

    @RequestMapping("/data-reports/list")
    public void reportList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String entity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn from DataReportConfig" +
                " where (1=1) and (2=2)" +
                " order by name, modifiedOn desc";

        Object[][] array = queryListOfConfig(sql, entity, q);
        writeSuccess(response, array);
    }

    @RequestMapping("/data-reports/check-template")
    public void checkTemplate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String file = getParameterNotNull(request, "file");
        String entity = getParameterNotNull(request, "entity");

        File template = SysConfiguration.getFileOfData(file);
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

    @RequestMapping("/data-reports/preview")
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
