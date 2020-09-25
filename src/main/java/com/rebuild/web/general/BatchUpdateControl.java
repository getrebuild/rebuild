/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.MetadataGetting;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批量修改
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@Controller
@RequestMapping("/app/{entity}/batch-update/")
public class BatchUpdateControl extends BaseController {

    @PostMapping("submit")
    public void submit(@PathVariable String entity,
                       HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        Assert.isTrue(Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate), "没有权限");

        JSONObject requestData = (JSONObject) ServletUtils.getRequestJson(request);

        requestData.put("_dataRange", getIntParameter(request, "dr", 2));
        requestData.put("entity", entity);
        BulkContext bulkContext = new BulkContext(user, BizzPermission.UPDATE, requestData);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        ServiceSpec ies = Application.getService(entityMeta.getEntityCode());
        String taskid = ((EntityService) ies).bulkAsync(bulkContext);

        writeSuccess(response, taskid);
    }

    @GetMapping("fields")
    public void getFields(@PathVariable String entity, HttpServletResponse response) {
        Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> updatableFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (!field.isUpdatable()) continue;

            EasyMeta easyMeta = EasyMeta.valueOf(field);
            if (!easyMeta.isUpdatable()) continue;

            DisplayType dt = easyMeta.getDisplayType();
            // 不支持的字段
            if (dt == DisplayType.FILE || dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
                    || dt == DisplayType.LOCATION || dt == DisplayType.SERIES || dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.NTEXT || dt == DisplayType.BARCODE || dt == DisplayType.N2NREFERENCE) {
                continue;
            }

            updatableFields.add(this.buildField(field, dt));
        }
        writeSuccess(response, updatableFields);
    }

    /**
     * @param field
     * @param dt
     * @return
     */
    private Map<String, Object> buildField(Field field, DisplayType dt) {
        Map<String, Object> map = MetadataGetting.buildField(field);

        // 字段选项
        if (dt == DisplayType.PICKLIST) {
            map.put("options", PickListManager.instance.getPickList(field));

        } else if (dt == DisplayType.STATE) {
            map.put("options", StateManager.instance.getStateOptions(field));

        } else if (dt == DisplayType.MULTISELECT) {
            map.put("options", MultiSelectManager.instance.getSelectList(field));

        } else if (dt == DisplayType.BOOL) {
            JSONArray options = new JSONArray();
            options.add(JSONUtils.toJSONObject(new String[]{"id", "text"}, new Object[]{true, "是"}));
            options.add(JSONUtils.toJSONObject(new String[]{"id", "text"}, new Object[]{false, "否"}));
            map.put("options", options);

        } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            map.put(FieldExtConfigProps.NUMBER_NOTNEGATIVE,
                    EasyMeta.valueOf(field).getExtraAttr(FieldExtConfigProps.NUMBER_NOTNEGATIVE));
        }

        return map;
    }
}
