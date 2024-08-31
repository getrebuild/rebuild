/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.data;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import cn.hutool.core.io.file.FileNameUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.esotericsoftware.minlog.Log;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.EasyExcelGenerator33;
import com.rebuild.core.service.datareport.EasyExcelListGenerator;
import com.rebuild.core.service.datareport.TemplateExtractor;
import com.rebuild.core.service.datareport.TemplateExtractor33;
import com.rebuild.core.service.datareport.TemplateFile;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.PdfConverter;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import com.rebuild.web.admin.ConfigCommons;
import com.rebuild.web.commons.FileDownloader;
import org.apache.commons.io.FileUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.rebuild.core.service.datareport.TemplateExtractor.NROW_PREFIX;

/**
 * Excel 报表
 *
 * @author devezhao
 * @since 2019/8/13
 * @see com.rebuild.web.general.ReportsController
 */
@RestController
@RequestMapping("/admin/data/")
public class ReportTemplateController extends BaseController {

    @GetMapping("/report-templates")
    public ModelAndView page() {
        return createModelAndView("/admin/data/report-templates");
    }

    @GetMapping("/report-templates/list")
    public RespBody reportList(HttpServletRequest request) {
        String entity = getParameter(request, "entity");
        String q = getParameter(request, "q");

        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn,templateType,extraDefinition,configId,templateFile from DataReportConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] list = ConfigCommons.queryListOfConfig(sql, entity, q);
        for (Object[] o : list) {
            JSONObject extra = o[7] == null ? JSONUtils.EMPTY_OBJECT : JSON.parseObject((String) o[7]);
            o[7] = extra;
        }

        return RespBody.ok(list);
    }

    @RequestMapping("/report-templates/check-template")
    public RespBody checkTemplate(@EntityParam Entity entity, HttpServletRequest request) {
        final String file = getParameterNotNull(request, "file");
        final int type = getIntParameter(request, "type", DataReportManager.TYPE_RECORD);

        boolean isDocx = file.toLowerCase().endsWith(".docx");
        if (type == DataReportManager.TYPE_WORD) {
            if (!isDocx) return RespBody.errorl("上传 WORD 文件请选择 WORD 模板类型");
        } else if (type != DataReportManager.TYPE_HTML5) {
            if (isDocx) return RespBody.errorl("上传 EXCEL 文件请选择 EXCEL 模板类型");
        }

        File template = type == DataReportManager.TYPE_HTML5
                ? null : RebuildConfiguration.getFileOfData(file);
        Map<String, String> vars = null;
        try {
            if (type == DataReportManager.TYPE_RECORD) {
                vars = new TemplateExtractor33(template).transformVars(entity);
            } else if (type == DataReportManager.TYPE_LIST) {
                vars = new TemplateExtractor(template, Boolean.TRUE).transformVars(entity);
            } else if (type == DataReportManager.TYPE_WORD) {
                //noinspection unchecked
                vars = (Map<String, String>) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.WordTemplateExtractor#transformVars", template, entity.getName());
            } else if (type == DataReportManager.TYPE_HTML5) {
                String templateContent = ServletUtils.getRequestString(request);
                //noinspection unchecked
                vars = (Map<String, String>) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.Html5TemplateExtractor#transformVars", templateContent, entity.getName());
            }

        } catch (Exception ex) {
            Log.error(null, ex);
            return RespBody.error(Language.L("无效模板文件 (无法读取文件内容)"));
        }

        if (vars == null || vars.isEmpty()) {
            return RespBody.error(Language.L("无效模板文件 (未找到有效字段)"));
        }

        String invalidMsg = null;
        if (type == DataReportManager.TYPE_LIST) {
            invalidMsg = Language.L("这可能不是一个有效的列表模板");
            for (String varName : vars.keySet()) {
                if (varName.startsWith(NROW_PREFIX)) {
                    invalidMsg = null;
                    break;
                }
            }
        }

        Set<String> invalidVars = new HashSet<>();
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getValue() == null && !TemplateExtractor33.isPlaceholder(e.getKey())) {
                invalidVars.add(e.getKey());
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

    @RequestMapping("/report-templates/preview")
    public ModelAndView preview(@IdParam(required = false) ID reportId,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        final TemplateFile tt;
        // 新建时
        if (reportId == null) {
            String entity = getParameter(request, "entity");
            String template = getParameter(request, "file");
            int type = getIntParameter(request, "type", DataReportManager.TYPE_RECORD);
            tt = new TemplateFile(RebuildConfiguration.getFileOfData(template), MetadataHelper.getEntity(entity), type, true, null);
        } else {
            // 使用配置
            tt = DataReportManager.instance.buildTemplateFile(reportId);
        }

        String sql = String.format("select %s from %s order by modifiedOn desc",
                tt.entity.getPrimaryField().getName(), tt.entity.getName());
        Object[] random = Application.createQueryNoFilter(sql).unique();
        if (random == null) {
            response.sendError(400, Language.L("未找到可供预览的记录"));
            return null;
        }

        File output;
        try {
            // EXCEL 列表
            if (tt.type == DataReportManager.TYPE_LIST) {
                JSONObject queryData = JSONUtils.toJSONObject(
                        new String[] { "pageSize", "entity" },
                        new Object[] { 2, tt.entity.getName() });
                output = EasyExcelListGenerator.create(tt.templateFile, queryData).generate();
            }
            // WORD
            else if (tt.type == DataReportManager.TYPE_WORD) {
                EasyExcelGenerator33 word = (EasyExcelGenerator33) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.WordReportGenerator#create", tt.templateFile, random[0]);
                output = word.generate();
            }
            // HTML5
            else if (tt.type == DataReportManager.TYPE_HTML5) {
                // 实时内容
                String templateContent = request.getParameter("templateContent");
                if (templateContent == null) templateContent = tt.templateContent;
                EasyExcelGenerator33 html5 = (EasyExcelGenerator33) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.Html5ReportGenerator#create", templateContent, random[0]);
                output = html5.generate();
            }
            // EXCEL
            else {
                output = EasyExcelGenerator.create(tt.templateFile, (ID) random[0], tt.isV33).generate();
            }

        } catch (ConfigurationException ex) {
            response.sendError(500, ex.getLocalizedMessage());
            return null;
        }

        RbAssert.is(output != null, Language.L("无法输出报表，请检查报表模板是否有误"));

        String attname = "RBREPORT-PREVIEW";

        // v3.6
        if (tt.type == DataReportManager.TYPE_HTML5) {
            return buildHtml5ModelAndView(output, attname);
        }

        attname += "." + FileNameUtil.getSuffix(output);
        String typeOutput = getParameter(request, "output");
        boolean forceInline = false;
        if (PdfConverter.TYPE_PDF.equalsIgnoreCase(typeOutput) || PdfConverter.TYPE_HTML.equalsIgnoreCase(typeOutput)) {
            output = PdfConverter.convert(output.toPath(), typeOutput).toFile();
            forceInline = true;
        }

        FileDownloader.downloadTempFile(response, output, attname, forceInline);
        return null;
    }

    @GetMapping("/report-templates/download")
    public void download(@IdParam(required = false) ID reportId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        File template;
        if (reportId != null) {
            template = DataReportManager.instance.buildTemplateFile(reportId).templateFile;
        } else {
            String path = getParameterNotNull(request, "file");
            template = RebuildConfiguration.getFileOfData(path);
        }

        String attname = QiniuCloud.parseFileName(template.getName());

        FileDownloader.setDownloadHeaders(request, response, attname, false);
        FileDownloader.writeLocalFile(template, response);
    }

    // --

    private static String HTML5_INLINE_STYLE;
    /**
     * @param html5
     * @param title
     * @return
     * @throws IOException
     */
    public static ModelAndView buildHtml5ModelAndView(File html5, String title) throws IOException {
        String content = FileUtils.readFileToString(html5, StandardCharsets.UTF_8);
        ModelAndView mv = new ModelAndView("/admin/data/template5-view");
        mv.getModelMap().put("reportName", title);
        mv.getModelMap().put("reportContent", content);
        if (HTML5_INLINE_STYLE == null || Application.devMode()) {
            HTML5_INLINE_STYLE = CommonsUtils.getStringOfRes("/web/assets/css/template5-design-content.css");
        }
        mv.getModelMap().put("inlineStyle", HTML5_INLINE_STYLE);
        return mv;
    }
}
