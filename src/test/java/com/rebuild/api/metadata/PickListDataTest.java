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
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/10/15
 */
public class PickListDataTest extends TestSupport {

    @Test
    public void executePickList() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("entity", TestAllFields);
        reqParams.put("field", "PICKLIST");
        ApiContext apiContext = new ApiContext(reqParams);

        JSONObject ret = (JSONObject) new PickListData().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(ret));
        Assert.assertEquals(0, ret.getIntValue("error_code"));
    }

    @Test
    public void executeMultiSelect() {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("entity", TestAllFields);
        reqParams.put("field", "MULTISELECT");
        ApiContext apiContext = new ApiContext(reqParams);

        JSONObject ret = (JSONObject) new MultiSelectData().execute(apiContext);
        System.out.println(JSONUtils.prettyPrint(ret));
        Assert.assertEquals(0, ret.getIntValue("error_code"));
    }
}