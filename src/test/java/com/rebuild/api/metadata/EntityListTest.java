/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.api.ApiContext;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/5/14
 */
public class EntityListTest extends TestSupport {

    @Test
    public void execute() {
        Map<String, String> reqParams = new HashMap<>();
        ApiContext apiContext = new ApiContext(reqParams, null);

        JSONObject ret = (JSONObject) new EntityList().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(ret));
        Assertions.assertNotNull(ret.get("error_code"));
    }
}