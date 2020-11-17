/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.FormsManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/{entity}/")
public class FormDesignController extends BaseController {

    @GetMapping("form-design")
    public ModelAndView page(@PathVariable String entity, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/admin/metadata/form-design");
        MetaEntityController.setEntityBase(mv, entity);
        mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));

        ConfigBean config = FormsManager.instance.getFormLayout(entity, getRequestUser(request));
        if (config != null) {
            request.setAttribute("FormConfig", config.toJSON());
        }
        return mv;
    }

    @RequestMapping({"form-update"})
    public void sets(@PathVariable String entity,
                     HttpServletRequest request, HttpServletResponse response) {
        JSON formJson = ServletUtils.getRequestJson(request);

        // 修改字段名称
        Map<String, String> newLabels = new HashMap<>();
        JSONArray config = ((JSONObject) formJson).getJSONArray("config");
        for (Object o : config) {
            JSONObject item = (JSONObject) o;
            String newLabel = item.getString("__newLabel");
            if (StringUtils.isNotBlank(newLabel)) {
                newLabels.put(item.getString("field"), newLabel);
            }
            item.remove("__newLabel");
        }
        ((JSONObject) formJson).put("config", config);

        Record record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
        if (record.getPrimary() == null) {
            record.setString("shareTo", FormsManager.SHARE_ALL);
        }
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        if (!newLabels.isEmpty()) {
            List<Record> willUpdate = new ArrayList<>();
            Entity entityMeta = MetadataHelper.getEntity(entity);
            for (Map.Entry<String, String> e : newLabels.entrySet()) {
                Field fieldMeta = entityMeta.getField(e.getKey());
                EasyField fieldEasy = EasyMetaFactory.valueOf(fieldMeta);
                if (fieldEasy.isBuiltin() || fieldEasy.getMetaId() == null) {
                    continue;
                }

                Record fieldRecord = EntityHelper.forUpdate(fieldEasy.getMetaId(), UserService.SYSTEM_USER, false);
                fieldRecord.setString("fieldLabel", e.getValue());
                willUpdate.add(fieldRecord);
            }

            if (!willUpdate.isEmpty()) {
                Application.getCommonsService().createOrUpdate(willUpdate.toArray(new Record[0]), false);
                MetadataHelper.getMetadataFactory().refresh(false);
            }
        }

        writeSuccess(response);
    }
}
