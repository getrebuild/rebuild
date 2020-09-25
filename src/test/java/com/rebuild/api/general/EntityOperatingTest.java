/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.api.ApiContext;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityOperatingTest extends TestSupport {

    @Test
    public void executeCreateAndUpdateAndDelete() {
        Application.getSessionStore().set(UserService.SYSTEM_USER);

        // CREATE

        JSONObject data = new JSONObject();
        data.put(EntityRecordCreator.META_FIELD, JSONUtils.toJSONObject("entity", TestAllFields));
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
        data.put(EntityRecordCreator.META_FIELD, JSONUtils.toJSONObject(
                new String[]{"entity", "id"}, new String[]{TestAllFields, createRecordId}));
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
        apiContext = new ApiContext(reqParams, null, null, UserService.SYSTEM_USER);

        final JSONObject deleteResult = (JSONObject) new EntityDelete().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(deleteResult));
        Assert.assertNotNull(deleteResult.get("error_code"));

        // Not exists
        displayEntityGet(createRecordId);
    }

    private void displayEntityGet(String recordId) {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("id", recordId);
        reqParams.put("fields", EntityListTest.getAllFields(MetadataHelper.getEntity(TestAllFields)));
        ApiContext apiContext = new ApiContext(reqParams, null, null, UserService.SYSTEM_USER);

        JSONObject result = (JSONObject) new EntityGet().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(result));
    }
}