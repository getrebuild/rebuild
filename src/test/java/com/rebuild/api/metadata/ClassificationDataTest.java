/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.server.TestSupport;
import com.rebuild.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/5/14
 */
public class ClassificationDataTest extends TestSupport {

    @Test
    public void execute() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("entity", "TestAllFields");
        reqParams.put("field", "CLASSIFICATION");
        ApiContext apiContext = new ApiContext(reqParams, null);

        JSONObject ret = (JSONObject) new ClassificationData().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(ret));
        Assert.assertNotNull(ret.get("error_code"));
    }
}