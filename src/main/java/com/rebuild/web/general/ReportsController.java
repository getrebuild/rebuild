/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dataimport.DataExporter;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.EasyExcelGenerator;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.commons.FileDownloader;
import org.springframework.util.Assert;
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

    // 报表

    @RequestMapping("report/available")
    public JSON availableReports(@PathVariable String entity) {
        Entity entityMeta = MetadataHelper.getEntity(entity);
        return DataReportManager.instance.getReports(entityMeta);
    }

    @RequestMapping({"report/generate", "report/export"})
    public void reportGenerate(@PathVariable String entity,
                               @IdParam(name = "report") ID reportId,
                               @IdParam(name = "record") ID recordId,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        File report = new EasyExcelGenerator(reportId, recordId).generate();

        if (ServletUtils.isAjaxRequest(request)) {
            writeSuccess(response, JSONUtils.toJSONObject("file", report.getName()));

        } else {
            String attname = request.getParameter("attname");
            if (attname == null) {
                attname = report.getName();
            }

            FileDownloader.setDownloadHeaders(request, response, attname);
            FileDownloader.writeLocalFile(report, response);
        }
    }

    // 数据导出

    @RequestMapping("export/submit")
    public RespBody export(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        Assert.isTrue(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport),
                getLang(request, "NoOpPrivileges"));

        int dataRange = getIntParameter(request, "dr", BatchOperatorQuery.DR_PAGED);
        JSONObject queryData = (JSONObject) ServletUtils.getRequestJson(request);
        queryData = new BatchOperatorQuery(dataRange, queryData).wrapQueryData(DataExporter.MAX_ROWS, false);

        try {
            DataExporter exporter = (DataExporter) new DataExporter(queryData).setUser(user);
            File file = exporter.export();
            return RespBody.ok(file.getName());

        } catch (Exception ex) {
            return RespBody.error(ex.getLocalizedMessage());
        }
    }
}
