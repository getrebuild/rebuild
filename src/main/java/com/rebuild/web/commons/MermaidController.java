/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author Zixin (RB)
 * @since 2025/9/18
 */
@Controller
public class MermaidController extends BaseController {

    @RequestMapping("/commons/mermaid")
    public ModelAndView renderMermaid(HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/common/mermaid-chart");
        // in POST
        String data = ServletUtils.getRequestString(request);
        if (StringUtils.isBlank(data)) {
            // in URL
            data = request.getParameter("data");
            if (data != null) data = CodecUtils.urlDecode(data);
        }

        data = Objects.requireNonNull(data).trim();
        mv.getModel().put("mermaidData", data);
        return mv;
    }

    @RequestMapping("/commons/chart")
    public ModelAndView renderChart(HttpServletRequest request) {
        ID chartid = getIdParameter(request, "id");
        ConfigBean c = ChartManager.instance.getChart(chartid);

        String filter = getParameter(request, "filter");
        JSONObject filterJson = JSONUtils.wellFormat(filter) ? JSON.parseObject(filter) : null;
        if (filterJson != null) {
            filterJson.put("entity", ((JSONObject) c.getJSON("config")).getString("entity"));
            filterJson.put("filter_type", "list");
        }

        ModelAndView mv = createModelAndView("/common/chart");
        mv.getModel().put("chartId", chartid);
        mv.getModel().put("chartConfig", c.toJSONString());
        mv.getModel().put("chartFilter", filterJson == null ? null : filterJson.toJSONString());
        return mv;
    }
}
