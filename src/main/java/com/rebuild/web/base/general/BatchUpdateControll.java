/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.base.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.MultiSelectManager;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.helper.state.StateManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.metadata.entity.FieldExtConfigProps;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.ServiceSpec;
import com.rebuild.server.service.base.BulkContext;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import com.rebuild.web.base.MetadataGetting;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
@RequestMapping("/app/{entity}/")
public class BatchUpdateControll extends BaseControll {

    @RequestMapping("batch-update/submit")
    public void submit(@PathVariable String entity,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Assert.isTrue(Application.getSecurityManager().allow(user, ZeroEntry.AllowBatchUpdate), "没有权限");

        JSONObject requestData = (JSONObject) ServletUtils.getRequestJson(request);

        int dataRange = getIntParameter(request, "dr", 2);
        requestData.put("_dataRange", dataRange);
        requestData.put("entity", entity);
        BulkContext bulkContext = new BulkContext(user, BizzPermission.UPDATE, requestData);

        Entity entityMeta = MetadataHelper.getEntity(entity);
        ServiceSpec ies = Application.getService(entityMeta.getEntityCode());
        String taskid = ((EntityService) ies).bulkAsync(bulkContext);

        writeSuccess(response, taskid);
    }

    // 获取可更新字段
    @RequestMapping("batch-update/fields")
    public void getFields(@PathVariable String entity, HttpServletResponse response) throws IOException {
        Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> updatableFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (MetadataHelper.isSystemField(field) || !field.isUpdatable()) {
                continue;
            }

            EasyMeta easyMeta = EasyMeta.valueOf(field);
            if (!easyMeta.isUpdatable()) {
                continue;
            }

            DisplayType dt = easyMeta.getDisplayType();
            // 不支持的字段
            if (dt == DisplayType.FILE || dt == DisplayType.IMAGE || dt == DisplayType.AVATAR
                    || dt == DisplayType.LOCATION || dt == DisplayType.SERIES || dt == DisplayType.ANYREFERENCE
                    || dt == DisplayType.NTEXT) {
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
            options.add(JSONUtils.toJSONObject(new String[] { "id", "text" }, new Object[] { true, "是"}));
            options.add(JSONUtils.toJSONObject(new String[] { "id", "text" }, new Object[] { false, "否"}));
            map.put("options", options);
        } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            map.put(FieldExtConfigProps.NUMBER_NOTNEGATIVE,
                    EasyMeta.valueOf(field).getPropOfFieldExtConfig(FieldExtConfigProps.NUMBER_NOTNEGATIVE));
        }

        return map;
    }
}
