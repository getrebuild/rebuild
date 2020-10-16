/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2020/08/18
 */
public class RBStoreTest extends TestSupport {

    @Test
    public void fetchMetaschema() {
        JSON data = RBStore.fetchMetaschema("ACCOUNT-1.0.json");
        Assertions.assertNotNull(data);

        System.out.println(JSONUtils.prettyPrint(data));
    }

    @Test
    public void fetchBusinessModel() {
        JSON data = RBStore.fetchBusinessModel("index.json");
        System.out.println(JSONUtils.prettyPrint(data));
    }
}