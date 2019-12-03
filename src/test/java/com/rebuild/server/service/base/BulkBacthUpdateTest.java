/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.helper.datalist.BatchOperatorQuery;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/12/03
 */
public class BulkBacthUpdateTest extends TestSupportWithUser {

    @Test
    public void exec() {
        ID recordId = addRecordOfTestAllFields();
        JSONObject customData = createCustomData();
        customData.getJSONObject("queryData").put("_selected", recordId.toLiteral());

        JSONObject useSet = JSONUtils.toJSONObject(
                new String[] { "field", "op", "value" },
                new String[] { "text", BulkBacthUpdate.OP_SET, "BatchUpdate-" + System.currentTimeMillis() });
        JSONArray updateContents = new JSONArray();
        updateContents.add(useSet);
        customData.put("updateContents", updateContents);

        BulkContext bulkContext = new BulkContext(getSessionUser(), BizzPermission.UPDATE, customData);
        BulkBacthUpdate bacthUpdate = new BulkBacthUpdate(bulkContext, Application.getGeneralEntityService());
        TaskExecutors.run(bacthUpdate);
    }

    @Test
    public void prepareRecords() {
        JSONObject customData = createCustomData();

        // SELECTED
        BulkContext bulkContext = new BulkContext(getSessionUser(), BizzPermission.UPDATE, customData);
        ID[] ids = new BulkBacthUpdate(bulkContext, null).prepareRecords();
        System.out.println(Arrays.toString(ids));

        // PAGED
        customData.put("_dataRange", BatchOperatorQuery.DR_PAGED);
        bulkContext = new BulkContext(getSessionUser(), BizzPermission.UPDATE, customData);
        ids = new BulkBacthUpdate(bulkContext, null).prepareRecords();
        System.out.println(Arrays.toString(ids));
    }

    private JSONObject createCustomData() {
        JSONObject queryData = new JSONObject();
        queryData.put("entity", TEST_ENTITY);
        queryData.put("fields", new String[]{EntityHelper.OwningUser});
        queryData.put("advFilter", null);
        queryData.put("pageNo", 1);
        queryData.put("pageSize", 5);
        queryData.put("_selected", "992-016d7b7b9abd0018|992-016d7b7bae2600c8");

        JSONObject customData = JSONUtils.toJSONObject("queryData", queryData);
        customData.put("_dataRange", BatchOperatorQuery.DR_SELECTED);
        customData.put("entity", TEST_ENTITY);
        return customData;
    }
}