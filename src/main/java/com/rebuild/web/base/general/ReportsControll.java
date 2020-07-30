/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.base.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.dataimport.DataExporter;
import com.rebuild.server.business.datareport.EasyExcelGenerator;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.configuration.portals.FormsBuilder;
import com.rebuild.server.helper.datalist.BatchOperatorQuery;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.common.FileDownloader;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 报表/打印
 *
 * @author devezhao
 * @since 2019/8/3
 */
@Controller
@RequestMapping("/app/{entity}/")
public class ReportsControll extends BasePageControll {

    // 打印视图

    @RequestMapping("print")
    public ModelAndView printPreview(@PathVariable String entity, HttpServletRequest request) {
        ID user = getRequestUser(request);
        ID recordId = getIdParameterNotNull(request, "id");

        JSON model = FormsBuilder.instance.buildView(entity, user, recordId);

        ModelAndView mv = createModelAndView("/general-entity/print-preview.jsp");
        mv.getModel().put("contentBody", model);
        mv.getModel().put("recordId", recordId);
        mv.getModel().put("printTime", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        mv.getModel().put("printUser", UserHelper.getName(user));
        return mv;
    }

    // 报表

    @RequestMapping("reports/available")
    public void availableReports(@PathVariable String entity, HttpServletResponse response) {
        Entity entityMeta = MetadataHelper.getEntity(entity);
        JSONArray reports = DataReportManager.instance.getReports(entityMeta);
        writeSuccess(response, reports);
    }

    @RequestMapping({ "reports/generate", "reports/export" })
    public void reportGenerate(@PathVariable String entity,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID reportId = getIdParameterNotNull(request, "report");
        ID recordId = getIdParameterNotNull(request, "record");

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

    // 列表导出

    @RequestMapping("data-export/submit")
    public void export(@PathVariable String entity,
                       HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        Assert.isTrue(Application.getPrivilegesManager().allow(user, ZeroEntry.AllowDataExport), "没有权限");

        int dataRange = getIntParameter(request, "dr", BatchOperatorQuery.DR_PAGED);
        JSONObject queryData = (JSONObject) ServletUtils.getRequestJson(request);
        queryData = new BatchOperatorQuery(dataRange, queryData).wrapQueryData(DataExporter.MAX_ROWS, false);

        try {
            File file = new DataExporter(queryData).setUser(user).export();
            writeSuccess(response, file.getName());
        } catch (Exception ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }
}
