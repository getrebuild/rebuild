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
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.PdfConverter;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

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

    @RequestMapping("report/available")
    public JSON availableReports(@PathVariable String entity, HttpServletRequest request) {
        JSONArray res = DataReportManager.instance.getReports(
                MetadataHelper.getEntity(entity),
                getIntParameter(request, "type", DataReportManager.TYPE_RECORD));

        // 名称排序
        res.sort((o1, o2) -> {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            return j1.getString("name").compareTo(j2.getString("name"));
        });
        return res;
    }

    @RequestMapping({"report/generate", "report/export"})
    public void reportGenerate(@PathVariable String entity,
                               @IdParam(name = "report") ID reportId,
                               @IdParam(name = "record") ID recordId,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        File output = null;
        try {
            output = new EasyExcelGenerator(reportId, recordId).generate();
        } catch (ExcelRuntimeException ex) {
            log.error(null, ex);
        }

        RbAssert.is(output != null, Language.L("无法输出报表，请检查报表模板是否有误"));

        final String outputType = getParameter(request, "output");
        // PDF
        if ("pdf".equals(outputType) || isOnlyPdf(entity, reportId)) {
            output = PdfConverter.convert(output.toPath()).toFile();
        }

        final String fileName = getReportName(entity, reportId, recordId, output);

        if (ServletUtils.isAjaxRequest(request)) {
            JSON data = JSONUtils.toJSONObject(
                    new String[] { "fileKey", "fileName" }, new Object[] { output.getName(), fileName });
            writeSuccess(response, data);

        } else if ("preview".equalsIgnoreCase(outputType)) {
            String fileUrl = String.format(
                    "/filex/download/%s?temp=yes&_onceToken=%s",
                    CodecUtils.urlEncode(output.getName()), AuthTokenManager.generateOnceToken(null));
            fileUrl = RebuildConfiguration.getHomeUrl(fileUrl);

            String previewUrl = StringUtils.defaultIfBlank(
                    RebuildConfiguration.get(ConfigurationItem.PortalOfficePreviewUrl),
                    "https://view.officeapps.live.com/op/embed.aspx?src=");

            previewUrl += CodecUtils.urlEncode(fileUrl);
            response.sendRedirect(previewUrl);

        } else {
            FileDownloader.downloadTempFile(response, output, fileName);
        }
    }

    // 列表数据导出

    @RequestMapping("export/submit")
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
                if ("pdf".equals(getParameter(request, "output")) || isOnlyPdf(entity, useReport)) {
                    output = PdfConverter.convert(output.toPath()).toFile();
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
                fileName = getReportName(entity, useReport, null, output);
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

    private String getReportName(String entity, ID reportId, ID recordId, File file) {
        String name = null;
        for (ConfigBean cb : DataReportManager.instance.getReportsRaw(MetadataHelper.getEntity(entity))) {
            if (cb.getID("id").equals(reportId)) {
                name = cb.getString("name");
                if (recordId == null || ContentWithFieldVars.matchsVars(name).isEmpty()) {
                    name = String.format("%s-%s", name, CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()));
                } else {
                    name = ContentWithFieldVars.replaceWithRecord(name, recordId);
                }

                // suffix
                if (file.getName().endsWith(".pdf")) name += ".pdf";
                else name += file.getName().endsWith(".xlsx") ? ".xlsx" : ".xls";
                break;
            }
        }

        return StringUtils.defaultIfBlank(name, "UNTITLE");
    }

    private boolean isOnlyPdf(String entity, ID reportId) {
        for (ConfigBean cb : DataReportManager.instance.getReportsRaw(MetadataHelper.getEntity(entity))) {
            if (cb.getID("id").equals(reportId)) {
                return "pdf".equalsIgnoreCase(cb.getString("outputType"));
            }
        }
        return false;
    }
}
