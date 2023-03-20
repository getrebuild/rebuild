/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.data;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
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
import com.rebuild.utils.RbAssert;
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

        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn,templateType,extraDefinition from DataReportConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] list = ConfigCommons.queryListOfConfig(sql, entity, q);
        for (Object[] o : list) {
            JSONObject extra = JSON.parseObject((String) o[7]);
            if (extra != null) o[7] = extra.getString("outputType");
        }

        return RespBody.ok(list);
    }

    @RequestMapping("/report-templates/check-template")
    public RespBody checkTemplate(@EntityParam Entity entity, HttpServletRequest request) {
        String file = getParameterNotNull(request, "file");
        boolean isList = getBoolParameter(request, "list");  // 列表模板

        File template = RebuildConfiguration.getFileOfData(file);
        Map<String, String> vars = new TemplateExtractor(template, isList).transformVars(entity);
        if (vars.isEmpty()) {
            return RespBody.error(Language.L("无效模板文件 (未找到有效字段)"));
        }

        String invalidMsg = null;
        if (isList) {
            invalidMsg = Language.L("这可能不是一个有效的列表模板");
            for (String varName : vars.keySet()) {
                if (varName.startsWith(TemplateExtractor.NROW_PREFIX)) {
                    invalidMsg = null;
                    break;
                }
            }
        }

        Set<String> invalidVars = new HashSet<>();
        for (Map.Entry<String, String> e : vars.entrySet()) {
            String varName = e.getKey();
            if (e.getValue() == null) {
                if (!(varName.startsWith(TemplateExtractor.PLACEHOLDER)
                        || varName.startsWith(TemplateExtractor.NROW_PREFIX + TemplateExtractor.PLACEHOLDER))) {
                    invalidVars.add(e.getKey());
                }
            }
        }

        if (invalidVars.size() >= vars.size()) {
            return RespBody.error(Language.L("无效模板文件 (未找到有效字段)"));
        }

        JSON res = JSONUtils.toJSONObject(
                new String[] { "invalidVars", "invalidMsg" },
                new Object[] { invalidVars, invalidMsg });
        return RespBody.ok(res);
    }

    @GetMapping("/report-templates/preview")
    public void preview(@IdParam(required = false) ID reportId,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        String entity = getParameter(request, "entity");
        String template = getParameter(request, "file");
        boolean isList = getBoolParameter(request, "list");

        // 使用配置
        if (reportId != null) {
            Object[] report = Application.createQueryNoFilter(
                    "select belongEntity,templateType,template from DataReportConfig where configId = ?")
                    .setParameter(1, reportId)
                    .unique();
            entity = (String) report[0];
            template = (String) report[2];
            isList = ObjectUtils.toInt(report[1]) == DataReportManager.TYPE_LIST;
        }

        Entity entityMeta = MetadataHelper.getEntity(entity);
        File templateFile = RebuildConfiguration.getFileOfData(template);

        String sql = String.format("select %s from %s order by modifiedOn desc",
                entityMeta.getPrimaryField().getName(), entityMeta.getName());
        Object[] random = Application.createQueryNoFilter(sql).unique();
        if (random == null) {
            response.sendError(400, Language.L("未找到可供预览的记录"));
            return;
        }

        File output;
        try {
            // 列表报表
            if (isList) {
                JSONObject queryData = JSONUtils.toJSONObject(
                        new String[] { "pageSize", "entity" },
                        new Object[] { 2, entity });
                output = new EasyExcelListGenerator(templateFile, queryData).generate();
            } else {
                output = new EasyExcelGenerator(templateFile, (ID) random[0]).generate();
            }

        } catch (ConfigurationException ex) {
            response.sendError(500, ex.getLocalizedMessage());
            return;
        }

        RbAssert.is(output != null, Language.L("无法输出报表，请检查报表模板是否有误"));
        FileDownloader.downloadTempFile(response, output, null);
    }

    @GetMapping("/report-templates/download")
    public void download(@IdParam ID reportId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        @SuppressWarnings("deprecation")
        File template = DataReportManager.instance.getTemplateFile(reportId);
        String attname = QiniuCloud.parseFileName(template.getName());

        FileDownloader.setDownloadHeaders(request, response, attname, false);
        FileDownloader.writeLocalFile(template, response);
    }
}
