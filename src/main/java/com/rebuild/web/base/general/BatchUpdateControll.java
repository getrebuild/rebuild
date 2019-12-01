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

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.dataimport.DataExporter;
import com.rebuild.server.helper.datalist.BatchOperatorQuery;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.web.BaseControll;
import com.rebuild.web.base.MetadataGetting;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO 批量修改
 *
 * @author ZHAO
 * @since 2019/12/1
 */
@Controller
@RequestMapping("/app/entity/")
public class BatchUpdateControll extends BaseControll {

    @RequestMapping("batch-update/submit")
    public void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Assert.isTrue(Application.getSecurityManager().allowed(user, ZeroEntry.AllowBatchUpdate), "没有权限");

        int dataRange = getIntParameter(request, "dr", 2);
        JSONObject queryData = (JSONObject) ServletUtils.getRequestJson(request);
        queryData = new BatchOperatorQuery(dataRange, queryData).wrapQueryData(DataExporter.MAX_ROWS);

        writeFailure(response);
    }

    // 获取可更新字段
    @RequestMapping("batch-update/fields")
    public void getFields(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String entity = getParameterNotNull(request, "entity");
        Entity entityMeta = MetadataHelper.getEntity(entity);

        List<Map<String, Object>> updatableFields = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entityMeta)) {
            if (MetadataHelper.isSystemField(field) || !field.isUpdatable()) {
                continue;
            }
            DisplayType dt = EasyMeta.getDisplayType(field);
            if (dt == DisplayType.FILE || dt == DisplayType.IMAGE || dt == DisplayType.AVATAR) {
                continue;
            }

            updatableFields.add(MetadataGetting.buildField(field));
        }
        writeSuccess(response, updatableFields);
    }
}
