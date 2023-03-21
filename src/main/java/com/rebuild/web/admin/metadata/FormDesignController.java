/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.FormsManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin (RB)
 * @since 08/19/2018
 */
@RestController
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
    public RespBody sets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);

        JSONArray config = ((JSONObject) formJson).getJSONArray("config");

        // 修改字段属性
        List<Record> willUpdates = new ArrayList<>();
        Entity entityMeta = MetadataHelper.getEntity(entity);
        for (Object o : config) {
            JSONObject item = (JSONObject) o;
            String newLabel = item.getString("__newLabel");
            Boolean newNullable = item.getBoolean("__newNullable");
            item.remove("__newLabel");
            item.remove("__newNullable");

            EasyField fieldEasy = EasyMetaFactory.valueOf(entityMeta.getField(item.getString("field")));
            if (fieldEasy.getMetaId() == null) continue;

            Record fieldRecord = EntityHelper.forUpdate(fieldEasy.getMetaId(), user, Boolean.FALSE);
            if (StringUtils.isNotBlank(newLabel) && !newLabel.equals(fieldEasy.getLabel())) {
                fieldRecord.setString("fieldLabel", newLabel);
            }
            if (newNullable != null && newNullable != fieldEasy.isNullable()) {
                fieldRecord.setBoolean("nullable", newNullable);
            }

            if (!fieldRecord.isEmpty()) willUpdates.add(fieldRecord);
        }

        ((JSONObject) formJson).put("config", config);

        Record record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
        if (record.getPrimary() == null) {
            record.setString("shareTo", FormsManager.SHARE_ALL);
        }
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        if (!willUpdates.isEmpty()) {
            Application.getCommonsService().createOrUpdate(willUpdates.toArray(new Record[0]), Boolean.FALSE);
            MetadataHelper.getMetadataFactory().refresh();
        }

        return RespBody.ok();
    }
}
