/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.FormBuilderContextHolder;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 表单/视图
 *
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@RestController
@RequestMapping("/app/{entity}/")
public class GeneralModelController extends EntityController {

    @GetMapping("view/{id}")
    public ModelAndView pageView(@PathVariable String entity, @PathVariable ID id,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final Entity useEntity = GeneralListController.checkPageOfEntity(user, entity, response);
        if (useEntity == null) return null;

        ModelAndView mv;
        if (useEntity.getMainEntity() != null) {
            mv = createModelAndView("/general/detail-view", id, user);
        } else {
            mv = createModelAndView("/general/record-view", id, user);

            JSONObject vtab = ViewAddonsManager.instance.getViewTab(entity, user);
            mv.getModel().put("ViewTabs", vtab.getJSONArray("items"));
            mv.getModel().put("ViewTabsAutoExpand", vtab.getBooleanValue("autoExpand"));
            JSONObject vadd = ViewAddonsManager.instance.getViewAdd(entity, user);
            mv.getModel().put("ViewAdds", vadd.getJSONArray("items"));
        }

        // 记录转换
        JSON trans = TransformManager.instance.getTransforms(entity, user);
        mv.getModel().put("TransformTos", trans);

        mv.getModel().put("id", id);
        return mv;
    }

    @PostMapping("form-model")
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
        return FormsBuilder.instance.buildView(entity, getRequestUser(request), id);
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
}
