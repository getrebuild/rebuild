/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.FormBuilderContextHolder;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表单/视图
 *
 * @author Zixin (RB)
 * @since 08/22/2018
 */
@Slf4j
@RestController
@RequestMapping("/app/{entity}/")
public class GeneralModelController extends EntityController {

    @GetMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String entity, @PathVariable ID id,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final Entity useEntity = GeneralListController.checkPageOfEntity(user, entity, response);
        if (useEntity == null) return null;

        if (Application.devMode() && !Objects.equals(id.getEntityCode(), MetadataHelper.getEntity(entity).getEntityCode())) {
            log.warn("Entity and ID do not match : " + request.getRequestURI());
        }
        
        boolean isDetail = useEntity.getMainEntity() != null;
        ModelAndView mv;
        if (isDetail) {
            mv = createModelAndView("/general/detail-view", id, user);
        } else {
            mv = createModelAndView("/general/record-view", id, user);
        }
        // 视图扩展
        mv.getModel().putAll(getViewExtras(user, entity, isDetail));
        // 显示历史
        mv.getModel().put("ShowViewHistory", RebuildConfiguration.getBool(ConfigurationItem.ShowViewHistory));

        mv.getModel().put("id", id);
        return mv;
    }

    private Map<String, Object> getViewExtras(ID user, String entity, boolean isDetail) {
        Map<String, Object> extras = new HashMap<>();
        if (!isDetail) {
            // 视图相关项
            JSONObject vtab = ViewAddonsManager.instance.getViewTab(entity, user);
            extras.put("ViewTabs", vtab.getJSONArray("items"));
            extras.put("ViewTabsAutoExpand", vtab.getBooleanValue("autoExpand"));
            extras.put("ViewTabsAutoHide", vtab.getBooleanValue("autoHide"));
            JSONObject vadd = ViewAddonsManager.instance.getViewAdd(entity, user);
            extras.put("ViewAdds", vadd.getJSONArray("items"));
        }

        // 记录转换
        JSON trans = TransformManager.instance.getTransforms(entity, user);
        extras.put("TransformTos", trans);

        return extras;
    }

    @RequestMapping("form-model")
    public JSON entityForm(@PathVariable String entity, @IdParam(required = false) ID id,
                           HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final Entity metaEntity = MetadataHelper.getEntity(entity);

        JSON initialVal = null;
        if (id == null) {
            initialVal = ServletUtils.getRequestJson(request);
            if (initialVal != null) {
                // 新建明细记录时必须指定主实体
                String mainid = ((JSONObject) initialVal).getString(FormsBuilder.DV_MAINID);
                if (ID.isId(mainid)) {
                    FormBuilderContextHolder.setMainIdOfDetail(ID.valueOf(mainid));
                }
                // v2.8
                else if (FormsBuilder.DV_MAINID.equals(mainid)) {
                    ID fakeMainid = EntityHelper.newUnsavedId(metaEntity.getMainEntity().getEntityCode());
                    FormBuilderContextHolder.setMainIdOfDetail(fakeMainid);
                }
            }
        }

        try {
            JSON model = FormsBuilder.instance.buildForm(entity, user, id);
            // 填充前端设定的初始值
            if (id == null && initialVal != null) {
                FormsBuilder.instance.setFormInitialValue(metaEntity, model, (JSONObject) initialVal);
            }
            return model;

        } finally {
            FormBuilderContextHolder.clear();
        }
    }

    @GetMapping("view-model")
    public JSON entityView(@PathVariable String entity, @IdParam ID id,
                           HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSONObject model = (JSONObject) FormsBuilder.instance.buildView(entity, user, id);

        // 返回扩展
        if (getBoolParameter(request, "extras") && !model.containsKey("error")) {
            Entity e = MetadataHelper.getEntity(entity);
            model.putAll(getViewExtras(user, entity, e.getMainEntity() != null));
            model.put("entityPrivileges", buildEntityPrivileges(id, user));
            model.put("entityLabel", Language.L(e));
            model.put("isDetail", e.getMainEntity() != null);
        }
        return model;
    }

    // 打印视图
    @GetMapping("print")
    public ModelAndView printPreview(@PathVariable String entity, @IdParam ID recordId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSON model = FormsBuilder.instance.buildView(entity, user, recordId);

        ModelAndView mv = createModelAndView("/general/print-preview");
        mv.getModel().put("contentBody", model);
        mv.getModel().put("recordId", recordId);
        mv.getModel().put("printTime", CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        mv.getModel().put("printUser", UserHelper.getName(user));
        return mv;
    }

    @RequestMapping("detail-models")
    public JSON entityFormDetails(@PathVariable String entity, @IdParam(name = "mainid") ID id,
                           HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final Entity metaEntity = MetadataHelper.getEntity(entity);

        Field dtf = MetadataHelper.getDetailToMainField(metaEntity);
        String sql = String.format("select %s from %s where %s = ? order by autoId asc",
                metaEntity.getPrimaryField().getName(), metaEntity.getName(), dtf.getName());
        Object[][] ids = Application.createQuery(sql).setParameter(1, id).array();
        
        JSONArray details = new JSONArray();
        for (Object[] o : ids) {
            JSON model = FormsBuilder.instance.buildForm(entity, user, (ID) o[0]);
            ((JSONObject) model).put("id", o[0]);
            details.add(model);
        }
        return details;
    }
}
