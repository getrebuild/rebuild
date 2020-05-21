/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityOperateTest extends TestSupportWithUser {

    @Test
    public void executeCreateAndUpdateAndDelete() {

        // CREATE

        JSONObject data = new JSONObject();
        data.put(ExtRecordCreator.META_FIELD, JSONUtils.toJSONObject("entity", TEST_ENTITY));
        data.put("TestAllFieldsName", "EntityCreateTest" + System.currentTimeMillis());

        Map<String, String> reqParams = new HashMap<>();
        ApiContext apiContext = new ApiContext(reqParams, data);

        final JSONObject createResult = (JSONObject) new EntityCreate().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(createResult));
        Assert.assertNotNull(createResult.get("error_code"));

        final String createRecordId = createResult.getJSONObject("data").getString("id");

        displayEntityGet(createRecordId);

        // UPDATE

        data = new JSONObject();
        data.put(ExtRecordCreator.META_FIELD, JSONUtils.toJSONObject(
                new String[] { "entity", "id" }, new String[] { TEST_ENTITY, createRecordId }));
        data.put("TestAllFieldsName", "UPDATE >> EntityCreateTest" + System.currentTimeMillis());

        reqParams = new HashMap<>();
        apiContext = new ApiContext(reqParams, data);

        final JSONObject updateResult = (JSONObject) new EntityUpdate().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(updateResult));
        Assert.assertNotNull(updateResult.get("error_code"));

        displayEntityGet(createRecordId);

        // DELETE

        reqParams = new HashMap<>();
        reqParams.put("id", createRecordId);
        apiContext = new ApiContext(reqParams, null, null, getSessionUser());

        final JSONObject deleteResult = (JSONObject) new EntityDelete().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(deleteResult));
        Assert.assertNotNull(deleteResult.get("error_code"));

        // Not exists
        displayEntityGet(createRecordId);
    }

    private void displayEntityGet(String recordId) {
        JSONObject data = new JSONObject();
        data.put(ExtRecordCreator.META_FIELD, JSONUtils.toJSONObject("entity", TEST_ENTITY));
        data.put("TestAllFieldsName", "EntityCreateTest" + System.currentTimeMillis());

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("id", recordId);
        reqParams.put("fields", EntityListTest.getAllFields(getTestEntity()));
        ApiContext apiContext = new ApiContext(reqParams, null, null, getSessionUser());

        JSONObject result = (JSONObject) new EntityGet().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(result));
    }
}