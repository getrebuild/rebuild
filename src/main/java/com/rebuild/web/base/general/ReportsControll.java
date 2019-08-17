/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.base.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.business.datareport.ReportGenerator;
import com.rebuild.server.configuration.DataReportManager;
import com.rebuild.server.configuration.portals.FormsBuilder;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.common.FileDownloader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 视图打印
 *
 * @author devezhao
 * @since 2019/8/3
 */
@Controller
@RequestMapping("/app/entity/")
public class ReportsControll extends BasePageControll {

    @RequestMapping("print")
    public ModelAndView printPreview(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID recordId = getIdParameterNotNull(request, "id");
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        JSON model = FormsBuilder.instance.buildView(entity.getName(), user, recordId);

        ModelAndView mv = createModelAndView("/general-entity/print-preview.jsp");
        mv.getModel().put("contentBody", model);
        mv.getModel().put("recordId", recordId);
        mv.getModel().put("printTime", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        return mv;
    }

    @RequestMapping("available-reports")
    public void availableReports(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String entity = getParameterNotNull(request, "entity");
        JSONArray reports = DataReportManager.instance.getReports(MetadataHelper.getEntity(entity));
        writeSuccess(response, reports);
    }

    @RequestMapping("report-generate")
    public void generateReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        File report = generateReport(request);
        writeSuccess(response, JSONUtils.toJSONObject("file", report.getName()));
    }

    @RequestMapping("report-export")
    public void exportReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        File report = generateReport(request);
        String attname = request.getParameter("attname");
        if (attname == null) {
            attname = report.getName();
        }

        response.setHeader("Content-Disposition", "attachment;filename=" + attname);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        FileDownloader.writeLocalFile(report.getName(), true, response);
    }

    /**
     * @param request
     * @return
     */
    private File generateReport(HttpServletRequest request) {
        ID user = getRequestUser(request);
        ID reportId = getIdParameterNotNull(request, "report");
        ID recordId = getIdParameterNotNull(request, "record");

        ReportGenerator generator = new ReportGenerator(reportId, recordId);
        generator.setUser(user);
        File file = generator.generate();
        return file;
    }
}
