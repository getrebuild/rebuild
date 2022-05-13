/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dataimport.DataExporter;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.support.CommonsLog;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.commons.FileDownloader;
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
        File file = new EasyExcelGenerator(reportId, recordId).generate();
        String fileName = getReportName(entity, reportId, file);

        if (ServletUtils.isAjaxRequest(request)) {
            JSON data = JSONUtils.toJSONObject(
                    new String[] { "fileKey", "fileName" }, new Object[] { file.getName(), fileName });
            writeSuccess(response, data);

        } else {
            FileDownloader.downloadTempFile(response, file, fileName);
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

        ID reportId = getIdParameter(request, "report");

        try {
            DataExporter exporter = (DataExporter) new DataExporter(queryData).setUser(user);
            File file = exporter.export(reportId);

            String fileName = reportId != null ? getReportName(entity, reportId, file) : null;
            if (fileName == null) {
                fileName = String.format("%s-%s.csv",
                        EasyMetaFactory.getLabel(entity),
                        CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()));
            }

            CommonsLog.createLog(CommonsLog.TYPE_EXPORT, user, null,
                    String.format("%s:%d", entity, exporter.getExportCount()));

            JSON data = JSONUtils.toJSONObject(
                    new String[] { "fileKey", "fileName" }, new Object[] { file.getName(), fileName });
            return RespBody.ok(data);

        } catch (Exception ex) {
            return RespBody.error(ex.getLocalizedMessage());
        }
    }

    private String getReportName(String entity, ID report, File file) {
        for (ConfigBean cb : DataReportManager.instance.getReportsRaw(MetadataHelper.getEntity(entity))) {
            if (cb.getID("id").equals(report)) {
                return String.format("%s-%s.%s",
                        cb.getString("name"),
                        CalendarUtils.getPlainDateFormat().format(CalendarUtils.now()),
                        file.getName().endsWith(".xlsx") ? "xlsx" : "xls");
            }
        }
        return null;
    }
}
