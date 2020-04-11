/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.dashboard;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.business.charts.ChartData;
import com.rebuild.server.business.charts.ChartsException;
import com.rebuild.server.business.charts.ChartsFactory;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.ChartManager;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author devezhao
 * @since 12/15/2018
 */
@Controller
@RequestMapping("/dashboard")
public class ChartDataControll extends BaseControll {

	@RequestMapping("/chart-data")
	public void data(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID chartid = getIdParameterNotNull(request, "id");
		Map<String, Object> paramMap = new HashMap<>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			paramMap.put(e.getKey(), StringUtils.join(e.getValue(), ","));
		}

		JSON data;
		try {
			ChartData chart = ChartsFactory.create(chartid);
			data = chart.setExtraParams(paramMap).build();
		} catch (ChartsException ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
		
		writeSuccess(response, data);
	}

    /**
     * @param request
     * @param response
     * @throws IOException
     *
     * @see com.rebuild.server.helper.datalist.DefaultDataListControl
     */
    @RequestMapping("view-chart-sources")
    public void viewChartSources(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID id = getIdParameterNotNull(request, "id");

        ConfigEntry configEntry = ChartManager.instance.getChart(id);
        JSONObject config = (JSONObject) configEntry.getJSON("config");
        String sourceEntity = config.getString("entity");

        String url = MessageFormat.format("../app/{0}/list?via={1}", sourceEntity, id);
        response.sendRedirect(url);
    }
}
