/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.dashboard;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.dashboard.ChartConfigService;
import com.rebuild.core.service.dashboard.DashboardConfigService;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.service.dashboard.charts.ChartsFactory;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.EntityController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import com.rebuild.web.InvalidParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 12/09/2018
 */
@RestController
@RequestMapping("/dashboard")
public class ChartDesignController extends EntityController {

    @GetMapping("/chart-design")
    public ModelAndView pageDesign(@IdParam(required = false) ID chartId,
                                   @EntityParam(name = "source", required = false) Entity entity,
                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        RbAssert.isAllow(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowCustomChart),
                Language.L("无操作权限"));

        ModelAndView mv = createModelAndView("/dashboard/chart-design");

        if (chartId != null) {
            Object[] chart = Application.createQueryNoFilter(
                    "select belongEntity,title,config,createdBy from ChartConfig where chartId = ?")
                    .setParameter(1, chartId)
                    .unique();
            if (chart == null) {
                response.sendError(404, Language.L("未知图表"));
                return null;
            }
            if (!UserHelper.isAdmin(user) && !user.equals(chart[3])) {
                response.sendError(403, Language.L("无权操作他人图表"));
                return null;
            }

            mv.getModel().put("chartId", chartId);
            mv.getModel().put("chartTitle", chart[1]);
            mv.getModel().put("chartConfig", chart[2]);
            mv.getModel().put("chartOwningAdmin", UserHelper.isAdmin((ID) chart[3]));
            entity = MetadataHelper.getEntity((String) chart[0]);

        } else if (entity != null) {
            mv.getModel().put("chartConfig", JSONUtils.EMPTY_OBJECT_STR);
            mv.getModel().put("chartOwningAdmin", UserHelper.isAdmin(user));

        } else {
            throw new InvalidParameterException(Language.L("无效请求参数"));
        }

        if (!Application.getPrivilegesManager().allowRead(getRequestUser(request), entity.getEntityCode())) {
            response.sendError(403, Language.L("没有读取 %s 的权限", EasyMetaFactory.getLabel(entity)));
            return null;
        }

        putEntityMeta(mv, entity);

        // Fields
        List<String[]> fields = new ArrayList<>();
        putFields(fields, entity, null);
        for (Field field : MetadataSorter.sortFields(entity, DisplayType.REFERENCE)) {
            int entityCode = field.getReferenceEntity().getEntityCode();
            if (MetadataHelper.isBizzEntity(entityCode) || entityCode == EntityHelper.RobotApprovalConfig) {
                continue;
            }

            putFields(fields, field.getReferenceEntity(), field);
        }
        mv.getModel().put("availableFields", fields);

        return mv;
    }

    private void putFields(List<String[]> dest, Entity entity, Field parent) {
        for (Field field : MetadataSorter.sortFields(entity)) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            DisplayType dt = easyField.getDisplayType();
            if (dt == DisplayType.IMAGE
                    || dt == DisplayType.FILE
                    || dt == DisplayType.AVATAR
                    || dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.N2NREFERENCE
                    || dt == DisplayType.LOCATION
                    || dt == DisplayType.MULTISELECT
                    || dt == DisplayType.BARCODE
                    || dt == DisplayType.NTEXT
                    || dt == DisplayType.SERIES) {
                continue;
            }

            String type = "text";
            if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                type = "date";
            } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                type = "num";
            } else if (dt == DisplayType.CLASSIFICATION) {
                type = "clazz";
            }

            dest.add(new String[]{
                    (parent == null ? "" : (parent.getName() + ".")) + easyField.getName(),
                    (parent == null ? "" : (EasyMetaFactory.getLabel(parent) + ".")) + easyField.getLabel(),
                    type});
        }
    }

    @PostMapping("/chart-preview")
    public JSON dataPreview(HttpServletRequest request) {
        JSON config = ServletUtils.getRequestJson(request);
        ChartData chart = ChartsFactory.create((JSONObject) config, getRequestUser(request));
        return chart.build(true);
    }

    @PostMapping("/chart-save")
    public JSON chartSave(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);

        Record record = EntityHelper.parse((JSONObject) formJson, user);
        ID dashid = null;
        if (record.getPrimary() == null) {
            dashid = getIdParameterNotNull(request, "dashid");
        }
        record = Application.getBean(ChartConfigService.class).createOrUpdate(record);

        // 添加到仪表盘
        if (dashid != null) {
            Object[] dash = Application.createQueryNoFilter(
                    "select config from DashboardConfig where configId = ?")
                    .setParameter(1, dashid)
                    .unique();
            JSONArray config = JSON.parseArray((String) dash[0]);

            JSONObject item = JSONUtils.toJSONObject("chart", record.getPrimary());
            item.put("w", 4);
            item.put("h", 4);
            config.add(item);

            Record dashRecord = EntityHelper.forUpdate(dashid, getRequestUser(request));
            dashRecord.setString("config", config.toJSONString());
            Application.getBean(DashboardConfigService.class).createOrUpdate(dashRecord);
        }

        return JSONUtils.toJSONObject("id", record.getPrimary());
    }

    @RequestMapping("/chart-delete")
    public RespBody chartDelete(@IdParam ID chartId) {
        Application.getBean(ChartConfigService.class).delete(chartId);
        return RespBody.ok();
    }
}
