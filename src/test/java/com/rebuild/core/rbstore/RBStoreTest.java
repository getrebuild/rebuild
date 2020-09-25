/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSON;
import com.rebuild.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2020/08/18
 */
public class RBStoreTest {

    @Test
    public void testFetchMetaschema() {
        JSON data = RBStore.fetchMetaschema("ACCOUNT-1.0.json");
        Assert.assertNotNull(data);

        System.out.println(JSONUtils.prettyPrint(data));
    }
}