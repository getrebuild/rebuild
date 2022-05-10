/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.general.BatchOperatorQuery;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/12/03
 */
public class BulkBacthUpdateTest extends TestSupport {

    @BeforeEach
    public void setUpPerMethod() {
        UserContextHolder.setUser(UserService.ADMIN_USER);
    }

    @Test
    public void exec() {
        ID recordId = addRecordOfTestAllFields(SIMPLE_USER);
        JSONObject customData = createCustomData();
        customData.getJSONObject("queryData").put("_selected", recordId.toLiteral());

        JSONObject useSet = JSONUtils.toJSONObject(
                new String[]{"field", "op", "value"},
                new String[]{"text", BulkBacthUpdate.OP_SET, "BatchUpdate-" + System.currentTimeMillis()});
        JSONArray updateContents = new JSONArray();
        updateContents.add(useSet);
        customData.put("updateContents", updateContents);

        BulkContext bulkContext = new BulkContext(SIMPLE_USER, BizzPermission.UPDATE, customData);
        BulkBacthUpdate bacthUpdate = new BulkBacthUpdate(bulkContext, Application.getGeneralEntityService());
        TaskExecutors.run(bacthUpdate);
    }

    @Test
    public void prepareRecords() {
        JSONObject customData = createCustomData();

        // SELECTED
        BulkContext bulkContext = new BulkContext(SIMPLE_USER, BizzPermission.UPDATE, customData);
        ID[] ids = new BulkBacthUpdate(bulkContext, null).prepareRecords();
        System.out.println(Arrays.toString(ids));

        // PAGED
        customData.put("_dataRange", BatchOperatorQuery.DR_PAGED);
        bulkContext = new BulkContext(SIMPLE_USER, BizzPermission.UPDATE, customData);
        ids = new BulkBacthUpdate(bulkContext, null).prepareRecords();
        System.out.println(Arrays.toString(ids));
    }

    private JSONObject createCustomData() {
        JSONObject queryData = new JSONObject();
        queryData.put("entity", TestAllFields);
        queryData.put("fields", new String[]{EntityHelper.OwningUser});
        queryData.put("advFilter", null);
        queryData.put("pageNo", 1);
        queryData.put("pageSize", 5);
        queryData.put("_selected", "992-016d7b7b9abd0018|992-016d7b7bae2600c8");

        JSONObject customData = JSONUtils.toJSONObject("queryData", queryData);
        customData.put("_dataRange", BatchOperatorQuery.DR_SELECTED);
        customData.put("entity", TestAllFields);
        return customData;
    }
}