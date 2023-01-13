/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.dashboard;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 12/15/2018
 */
@RestController
@RequestMapping("/dashboard")
public class ChartDataController extends BaseController {

    @RequestMapping("/chart-data")
    public JSON data(@IdParam ID chartId, HttpServletRequest request) {
        Map<String, Object> extraParams = new HashMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            extraParams.put(e.getKey(), StringUtils.join(e.getValue(), ","));
        }

        // v3.2
        JSON config = ServletUtils.getRequestJson(request);
        if (config instanceof JSONObject) {
            JSON extconfig = ((JSONObject) config).getJSONObject("extconfig");
            if (extconfig != null) extraParams.put("extconfig", extconfig);
        }

        return ChartsFactory.create(chartId)
                .setExtraParams(extraParams)
                .build();
    }

    /**
     * @param response
     * @throws IOException
     * @see DataListBuilderImpl
     */
    @RequestMapping("view-chart-source")
    public void viewChartSource(@IdParam ID chartId, HttpServletResponse response) throws IOException {
        ConfigBean configEntry = ChartManager.instance.getChart(chartId);

        JSONObject config = (JSONObject) configEntry.getJSON("config");
        String sourceEntity = config.getString("entity");

        String url = MessageFormat.format("../app/{0}/list#via={1}", sourceEntity, chartId);
        response.sendRedirect(url);
    }
}
