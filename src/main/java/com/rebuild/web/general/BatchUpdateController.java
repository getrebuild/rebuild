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
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateManager;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量修改
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@RestController
@RequestMapping("/app/{entity}/batch-update/")
public class BatchUpdateController extends BaseController {

    @PostMapping("submit")
    public RespBody submit(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        Assert.isTrue(
                Application.getPrivilegesManager().allow(user, ZeroEntry.AllowBatchUpdate),
                getLang(request, "NoOpPrivileges"));

        JSONObject requestData = (JSONObject) ServletUtils.getRequestJson(request);

        requestData.put("_dataRange", getIntParameter(request, "dr", 2));
        requestData.put("entity", entity);
        BulkContext bulkContext = new BulkContext(user, BizzPermission.UPDATE, requestData);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        String taskid = Application.getEntityService(entityMeta.getEntityCode()).bulkAsync(bulkContext);

        return RespBody.ok(taskid);
    }

    @GetMapping("fields")
    public List<JSONObject> getFields(@PathVariable String entity) {
        Entity entityMeta = MetadataHelper.getEntity(entity);

        List<JSONObject> updatableFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (!field.isUpdatable()) continue;

            EasyField easyMeta = EasyMetaFactory.valueOf(field);
            if (!easyMeta.isUpdatable()) continue;

            DisplayType dt = easyMeta.getDisplayType();
            // 不支持的字段
            if (dt == DisplayType.FILE
                    || dt == DisplayType.IMAGE
                    || dt == DisplayType.AVATAR
                    || dt == DisplayType.LOCATION
                    || dt == DisplayType.BARCODE
                    || dt == DisplayType.SERIES
                    || dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.N2NREFERENCE) {
                continue;
            }

            updatableFields.add(buildField(field, dt));
        }
        return updatableFields;
    }

    /**
     * @param field
     * @param dt
     * @return
     */
    private JSONObject buildField(Field field, DisplayType dt) {
        JSONObject map = EasyMetaFactory.getFieldShow(field);

        // 字段选项
        if (dt == DisplayType.PICKLIST) {
            map.put("options", PickListManager.instance.getPickList(field));

        } else if (dt == DisplayType.STATE) {
            map.put("options", StateManager.instance.getStateOptions(field));

        } else if (dt == DisplayType.MULTISELECT) {
            map.put("options", MultiSelectManager.instance.getSelectList(field));

        } else if (dt == DisplayType.BOOL) {
            JSONArray options = new JSONArray();
            options.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text" },
                    new Object[] { true, Language.L("True") }));
            options.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text" },
                    new Object[] { false, Language.L("False") }));
            map.put("options", options);

        } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            map.put(FieldExtConfigProps.NUMBER_NOTNEGATIVE,
                    EasyMetaFactory.valueOf(field).getExtraAttr(FieldExtConfigProps.NUMBER_NOTNEGATIVE));

        }
        return map;
    }
}
