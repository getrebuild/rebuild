/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.data;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.EasyExcelListGenerator;
import com.rebuild.core.service.datareport.TemplateExtractor;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import com.rebuild.web.admin.ConfigCommons;
import com.rebuild.web.commons.FileDownloader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@RestController
@RequestMapping("/admin/data/")
public class ReportTemplateController extends BaseController {

    @RequestMapping("/report-templates")
    public ModelAndView page() {
        return createModelAndView("/admin/data/report-templates");
    }

    @GetMapping("/report-templates/list")
    public RespBody reportList(HttpServletRequest request) {
        String entity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn,templateType from DataReportConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] list = ConfigCommons.queryListOfConfig(sql, entity, q);
        return RespBody.ok(list);
    }

    @RequestMapping("/report-templates/check-template")
    public RespBody checkTemplate(@EntityParam Entity entity, HttpServletRequest request) {
        String file = getParameterNotNull(request, "file");
        File template = RebuildConfiguration.getFileOfData(file);

        Map<String, String> vars = new TemplateExtractor(template, true).transformVars(entity);
        if (vars.isEmpty()) {
            return RespBody.error(Language.L("无效模板文件 (未找到有效字段)"));
        }

        Set<String> invalidVars = new HashSet<>();
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getValue() == null) {
                invalidVars.add(e.getKey());
            }
        }

        if (invalidVars.size() >= vars.size()) {
            return RespBody.error(Language.L("无效模板文件 (未找到有效字段)"));
        }

        return RespBody.ok(JSONUtils.toJSONObject("invalidVars", invalidVars));
    }

    @GetMapping("/report-templates/preview")
    public void preview(@IdParam ID reportId, HttpServletResponse response) throws IOException {
        Object[] report = Application.createQueryNoFilter(
                "select belongEntity,templateType from DataReportConfig where configId = ?")
                .setParameter(1, reportId)
                .unique();
        Entity entity = MetadataHelper.getEntity((String) report[0]);

        String sql = String.format("select %s from %s order by modifiedOn desc",
                entity.getPrimaryField().getName(), entity.getName());
        Object[] random = Application.createQueryNoFilter(sql).unique();
        if (random == null) {
            response.sendError(400, Language.L("未找到可供预览的记录"));
            return;
        }

        File file;
        try {
            // 列表报表
            if (ObjectUtils.toInt(report[1]) == DataReportManager.TYPE_LIST) {
                JSONObject queryData = JSONUtils.toJSONObject(
                        new String[] { "pageSize", "entity" },
                        new Object[] { 2, report[0] });
                file = new EasyExcelListGenerator(reportId, queryData).generate();
            } else {
                file = new EasyExcelGenerator(reportId, (ID) random[0]).generate();
            }

        } catch (ConfigurationException ex) {
            response.sendError(500, ex.getLocalizedMessage());
            return;
        }

        FileDownloader.downloadTempFile(response, file, null);
    }
}
