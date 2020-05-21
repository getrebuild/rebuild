/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityListTest extends TestSupportWithUser {

    @Test
    public void execute() {
        JSONObject advFilter = new JSONObject();

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("entity", TEST_ENTITY);
        reqParams.put("fields", getAllFields(getTestEntity()));
        reqParams.put("page_size", "1");
//        reqParams.put("q", "1");
        ApiContext apiContext = new ApiContext(reqParams, advFilter);

        final JSONObject listResult = (JSONObject) new EntityList().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(listResult));
        Assert.assertNotNull(listResult.get("error_code"));
    }

    static String getAllFields(Entity entity) {
        List<String> allFields = new ArrayList<>();
        for (Field field : entity.getFields()) {
            allFields.add(field.getName());
        }
        return StringUtils.join(allFields.iterator(), ",");
    }
}