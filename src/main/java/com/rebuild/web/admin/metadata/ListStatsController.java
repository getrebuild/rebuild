/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2021/6/22
 */
@RestController
@RequestMapping("/admin/entity/")
public class ListStatsController extends BaseController {

    @PostMapping("{entity}/list-stats")
    public RespBody sets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSON config = ServletUtils.getRequestJson(request);

        ID configId = DataListManager.instance.detectUseConfig(user, entity, DataListManager.TYPE_LISTSTATS);

        Record record;
        if (configId == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", DataListManager.TYPE_LISTSTATS);
            record.setString("shareTo", DataListManager.SHARE_ALL);
        } else {
            record = EntityHelper.forUpdate(configId, user);
        }
        record.setString("config", config.toJSONString());
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        return RespBody.ok();
    }

    @GetMapping("{entity}/list-stats")
    public JSON gets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ConfigBean config = DataListManager.instance.getListStats(user, entity);
        JSONObject configJson = config == null ? new JSONObject() : (JSONObject) config.getJSON("config");

        // 可用字段
        JSONArray afields = new JSONArray();
        for (Field field : MetadataSorter.sortFields(
                MetadataHelper.getEntity(entity), DisplayType.NUMBER, DisplayType.DECIMAL)) {
            afields.add(EasyMetaFactory.toJSON(field));
        }
        configJson.put("fields", afields);
        return configJson;
    }

    // -- FilterPane 借用

    @PostMapping("{entity}/list-filterpane")
    public RespBody sets2(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        JSON config = ServletUtils.getRequestJson(request);

        ID configId = DataListManager.instance.detectUseConfig(user, entity, DataListManager.TYPE_LISTFILTERPANE);

        Record record;
        if (configId == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", DataListManager.TYPE_LISTFILTERPANE);
            record.setString("shareTo", DataListManager.SHARE_ALL);
        } else {
            record = EntityHelper.forUpdate(configId, user);
        }
        record.setString("config", config.toJSONString());
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        return RespBody.ok();
    }

    @GetMapping("{entity}/list-filterpane")
    public JSON gets2(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ConfigBean config = DataListManager.instance.getListFilterPane(user, entity);
        JSONObject configJson = config == null ? new JSONObject() : (JSONObject) config.getJSON("config");

        // 可用字段
        JSONArray afields = new JSONArray();
        for (Field field : MetadataSorter.sortFields(MetadataHelper.getEntity(entity))) {
            EasyField easyField = EasyMetaFactory.valueOf(field);
            DisplayType dt = easyField.getDisplayType();
            if (dt == DisplayType.BARCODE || dt == DisplayType.SIGN
                    || dt == DisplayType.AVATAR || dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
                continue;
            }
            afields.add(EasyMetaFactory.toJSON(field));
        }

        configJson.put("fields", afields);
        return configJson;
    }
}
