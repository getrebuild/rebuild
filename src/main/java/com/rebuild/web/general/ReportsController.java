/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.exception.ExcelRuntimeException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dataimport.DataExporter;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.service.datareport.EasyExcelGenerator33;
import com.rebuild.core.service.datareport.TemplateFile;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.PdfConverter;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.admin.data.ReportTemplateController;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * 报表/导出
 *
 * @author devezhao
 * @since 2019/8/3
 */
@Slf4j
@RestController
@RequestMapping("/app/{entity}/")
public class ReportsController extends BaseController {

    // 报表模板

    @GetMapping("report/available")
    public JSON availableReports(@PathVariable String entity, HttpServletRequest request) {
        JSONArray alist = DataReportManager.instance.getReportTemplates(
                MetadataHelper.getEntity(entity),
                getIntParameter(request, "type", DataReportManager.TYPE_RECORD),
                getIdParameter(request, "record"));

        // 名称排序
        alist.sort((o1, o2) -> {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            return j1.getString("name").compareTo(j2.getString("name"));
        });
        return alist;
    }

    @RequestMapping({"report/generate", "report/export"})
    public ModelAndView reportGenerate(@PathVariable String entity,
                               @IdParam(name = "report") ID reportId,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID[] recordIds = getIdArrayParameterNotNull(request, "record");
        final ID recordId = recordIds[0];
        final TemplateFile tt = DataReportManager.instance.getTemplateFile(reportId);

        File output = null;
        try {
            if (tt.type == DataReportManager.TYPE_WORD) {
                EasyExcelGenerator33 word = (EasyExcelGenerator33) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.WordReportGenerator#create", reportId, recordId);
                output = word.generate();

            } else if (tt.type == DataReportManager.TYPE_HTML5) {
                EasyExcelGenerator33 html5 = (EasyExcelGenerator33) CommonsUtils.invokeMethod(
                        "com.rebuild.rbv.data.Html5ReportGenerator#create", reportId, recordId);
                output = html5.generate();

            } else {
                // EXCEL 支持多个
                output = EasyExcelGenerator.create(reportId, Arrays.asList(recordIds)).generate();
            }

        } catch (ExcelRuntimeException ex) {
            log.error(null, ex);
        }

        RbAssert.is(output != null, Language.L("无法输出报表，请检查报表模板是否有误"));

        String fileName = DataReportManager.getReportName(reportId, recordId, output.getName());

        // v3.6
        if (tt.type == DataReportManager.TYPE_HTML5) {
            return ReportTemplateController.buildHtml5ModelAndView(output, fileName);
        }

        final String typeOutput = getParameter(request, "output");
        final boolean isHtml = "HTML".equalsIgnoreCase(typeOutput);
        final boolean isPdf = "PDF".equalsIgnoreCase(typeOutput);
        if (isPdf || isOnlyPdf(entity, reportId)) {
            output = PdfConverter.convertPdf(output.toPath()).toFile();
            fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".pdf";
        } else if (isHtml) {
            output = PdfConverter.convertHtml(output.toPath()).toFile();
            fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".html";
        }

        if (ServletUtils.isAjaxRequest(request)) {
            JSONObject data = JSONUtils.toJSONObject(
                    new String[] { "fileKey", "fileName" }, new Object[] { output.getName(), fileName });

            if (AppUtils.isMobile(request)) {
                String fileUrl = String.format(
                        "/filex/download/%s?temp=yes&_csrfToken=%s&attname=%s",
                        CodecUtils.urlEncode(output.getName()), AuthTokenManager.generateCsrfToken(90), CodecUtils.urlEncode(fileName));
                data.put("fileUrl", fileUrl);
            }
            writeSuccess(response, data);

        } else if ("preview".equalsIgnoreCase(typeOutput)) {
            String fileUrl = String.format(
                    "/filex/download/%s?temp=yes&_onceToken=%s&attname=%s",
                    CodecUtils.urlEncode(output.getName()), AuthTokenManager.generateOnceToken(null), CodecUtils.urlEncode(fileName));
            fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);

            String previewUrl = StringUtils.defaultIfBlank(
                    RebuildConfiguration.get(ConfigurationItem.PortalOfficePreviewUrl), "https://view.officeapps.live.com/op/embed.aspx?src=");

            previewUrl += CodecUtils.urlEncode(fileUrl);
            response.sendRedirect(previewUrl);

        } else {
            // 直接预览
            boolean forcePreview = isHtml || isPdf || getBoolParameter(request, "preview");
            FileDownloader.downloadTempFile(response, output, fileName, forcePreview);
        }
        return null;
    }
    
    // 列表数据导出

    @RequestMapping({ "export/submit", "report/export-list" })
    public RespBody export(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport),
                Language.L("无操作权限"));

        int dataRange = getIntParameter(request, "dr", BatchOperatorQuery.DR_PAGED);
        JSONObject queryData = (JSONObject) ServletUtils.getRequestJson(request);
        queryData = new BatchOperatorQuery(dataRange, queryData).wrapQueryData(DataExporter.MAX_ROWS, false);

        // 导出格式
        String reportType = getParameter(request, "report");
        ID useReport = ID.isId(reportType) ? ID.valueOf(reportType) : null;

        try {
            DataExporter exporter = (DataExporter) new DataExporter(queryData).setUser(user);
            File output;
            if (useReport != null) {
                output = exporter.export(useReport);

                // PDF
                final String typeOutput = getParameter(request, "output");
                if ("PDF".equalsIgnoreCase(typeOutput) || isOnlyPdf(entity, useReport)) {
                    output = PdfConverter.convertPdf(output.toPath()).toFile();
                } else if ("HTML".equalsIgnoreCase(typeOutput)) {
                    output = PdfConverter.convertHtml(output.toPath()).toFile();
                }

            } else {
                output = exporter.export(reportType);
            }

            RbAssert.is(output != null, Language.L("无法输出报表，请检查报表模板是否有误"));
            
            String fileName;
            if (useReport == null) {
                fileName = String.format("%s-%s.%s",
                        EasyMetaFactory.getLabel(entity),
                        CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()),
                        FileUtil.getSuffix(output));
            } else {
                fileName = DataReportManager.getReportName(useReport, entity, output.getName());
            }

            CommonsLog.createLog(CommonsLog.TYPE_EXPORT, user, null,
                    String.format("%s:%d", entity, exporter.getExportCount()));

            JSON data = JSONUtils.toJSONObject(
                    new String[] { "fileKey", "fileName" }, new Object[] { output.getName(), fileName });
            return RespBody.ok(data);

        } catch (Exception ex) {
            log.error(null, ex);
            return RespBody.error(ex.getLocalizedMessage());
        }
    }

    // 是否只能导出PDF
    private boolean isOnlyPdf(String entity, ID reportId) {
        for (ConfigBean cb : DataReportManager.instance.getReportsRaw(MetadataHelper.getEntity(entity))) {
            if (cb.getID("id").equals(reportId)) {
                return "pdf".equalsIgnoreCase(cb.getString("outputType"));
            }
        }
        return false;
    }
}
