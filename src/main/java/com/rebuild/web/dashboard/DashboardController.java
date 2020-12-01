/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.dashboard;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.dashboard.DashboardConfigService;
import com.rebuild.core.service.dashboard.DashboardManager;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.service.dashboard.charts.builtin.BuiltinChart;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController extends BaseController {

    @GetMapping("/home")
    public ModelAndView pageHome(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ModelAndView mav = createModelAndView("/dashboard/home");
        mav.getModelMap().put(ZeroEntry.AllowCustomChart.name(),
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomChart));
        return mav;
    }

    @GetMapping("/dash-gets")
    public JSON dashGets(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        return DashboardManager.instance.getAvailable(user);
    }

    @PostMapping("/dash-new")
    public JSON dashNew(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSONObject formJson = (JSONObject) ServletUtils.getRequestJson(request);
        JSONArray dashCopy = formJson.getJSONArray("__copy");
        if (dashCopy != null) {
            formJson.remove("__copy");
        }
        formJson.put("config", JSONUtils.EMPTY_ARRAY_STR);

        Record dashRecord = EntityHelper.parse(formJson, user);

        // 复制当前面板
        if (dashCopy != null) {
            for (Object o : dashCopy) {
                JSONObject item = (JSONObject) o;
                Record chart = Application.createQueryNoFilter(
                        "select config,belongEntity,chartType,title,createdBy from ChartConfig where chartId = ?")
                        .setParameter(1, ID.valueOf(item.getString("chart")))
                        .record();
                if (chart == null) {
                    continue;
                }
                // 自己的直接使用，不是自己的复制一份
                if (UserHelper.isSelf(user, chart.getID("createdBy"))) {
                    continue;
                }

                chart.removeValue("createdBy");
                Record chartRecord = EntityHelper.forNew(EntityHelper.ChartConfig, user);
                for (Iterator<String> iter = chart.getAvailableFieldIterator(); iter.hasNext(); ) {
                    String field = iter.next();
                    chartRecord.setObjectValue(field, chart.getObjectValue(field));
                }
                chartRecord = Application.getCommonsService().create(chartRecord);
                item.put("chart", chartRecord.getPrimary());
            }
            dashRecord.setString("config", dashCopy.toJSONString());
        }

        dashRecord = Application.getBean(DashboardConfigService.class).create(dashRecord);

        return JSONUtils.toJSONObject("id", dashRecord.getPrimary());
    }

    @PostMapping("/dash-config")
    public RespBody dashConfig(@IdParam ID dashId, HttpServletRequest request) {
        JSON config = ServletUtils.getRequestJson(request);

        Record record = EntityHelper.forUpdate(dashId, getRequestUser(request));
        record.setString("config", config.toJSONString());
        Application.getBean(DashboardConfigService.class).update(record);

        return RespBody.ok();
    }

    @GetMapping("/chart-list")
    public JSONArray chartList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String type = request.getParameter("type");

        JSONArray charts;

        if ("BUILTIN".equalsIgnoreCase(type)) {
            charts = (JSONArray) JSONUtils.EMPTY_ARRAY.clone();

        } else {
            // 指定实体
            String[] specEntity = null;
            if ("ENTITY".equalsIgnoreCase(type)) {
                String entity = getParameterNotNull(request, "entity");
                Entity entityMeta = MetadataHelper.getEntity(entity);

                if (entityMeta.getMainEntity() != null) {
                    specEntity = new String[] { entity, entityMeta.getMainEntity().getName() };
                } else if (entityMeta.getDetailEntity() != null) {
                    specEntity = new String[] { entity, entityMeta.getDetailEntity().getName() };
                } else {
                    specEntity = new String[] { entity };
                }
            }

            charts = ChartManager.instance.getChartList(user, specEntity, "MYSELF".equalsIgnoreCase(type));
        }

        // 附加内置图表
        if (!("ENTITY".equalsIgnoreCase(type) || "MYSELF".equalsIgnoreCase(type))) {
            for (BuiltinChart b : ChartsFactory.getBuiltinCharts()) {
                charts.add(JSONUtils.toJSONObject(
                        new String[] { "id", "title", "type", "entityLabel" },
                        new Object[] { b.getChartId(), b.getChartTitle(), b.getChartType(), Language.L("BuiltIn") }));
            }
        }

        return charts;
    }
}
