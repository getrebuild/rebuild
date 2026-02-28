/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.dashboard;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.dashboard.ChartManager;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.service.dashboard.charts.builtin.FeedsSchedule;
import com.rebuild.core.service.dashboard.charts.builtin.MyNotification;
import com.rebuild.core.support.ShortUrls;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
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
    public JSON data(@IdParam ID chartId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ID requestUser = AppUtils.getRequestUser(request);
        if (requestUser == null) {
            // sk:xxx
            String shareKeyToken = request.getHeader(AppUtils.HF_CSRFTOKEN);
            String fileUrl = StringUtils.isBlank(shareKeyToken)
                    ? null : ShortUrls.retrieveUrl(shareKeyToken.substring(3));
            if (ID.isId(fileUrl) && ID.valueOf(fileUrl).getEntityCode() == EntityHelper.DashboardConfig) {
                requestUser = UserService.SYSTEM_USER;
                UserContextHolder.setUser(requestUser);
            }
        }
        if (requestUser == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return null;
        }

        try {
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

        } finally {
            if (UserService.SYSTEM_USER.equals(requestUser)) UserContextHolder.clearUser();
        }
    }

    @RequestMapping("view-chart-source")
    public void viewChartSource(@IdParam ID chartId,
                                HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (MyNotification.MYID.equals(chartId)) {
            response.sendRedirect("../notifications");
            return;
        }
        if (FeedsSchedule.MYID.equals(chartId)) {
            response.sendRedirect("../feeds/home");
            return;
        }

        ConfigBean configEntry = ChartManager.instance.getChart(chartId);

        JSONObject config = (JSONObject) configEntry.getJSON("config");
        String sourceEntity = config.getString("entity");

        String url = MessageFormat.format("../app/{0}/list#via={1}", sourceEntity, chartId);
        String axis = getParameter(request, "axis");
        if (StringUtils.isNotBlank(axis)) url += ":" + axis;
        response.sendRedirect(url);
    }

    @RequestMapping("entities-creates-reads")
    public JSON getCreatesAndReads(HttpServletRequest request) {
        ID user = getRequestUser(request);
        boolean isAdmin = UserHelper.isAdmin(user);

        JSONArray res = new JSONArray();
        for (Entity e : MetadataSorter.sortEntities()) {
            int eCode = e.getEntityCode();
            if (!MetadataHelper.isBusinessEntity(e)) {
                if (isAdmin && MetadataHelper.isBizzEntity(e)) {
                    // 管理员允许 BIZZ
                } else {
                    continue;
                }
            }

            boolean R = Application.getPrivilegesManager().allowRead(user, eCode);
            boolean C = Application.getPrivilegesManager().allowCreate(user, eCode);
            if (!R && !C) continue;

            JSONObject item = EasyMetaFactory.toJSON(e);
            item.put("read", R);
            item.put("create", e.getMainEntity() == null && C);
            res.add(item);
        }
        return res;
    }
}
