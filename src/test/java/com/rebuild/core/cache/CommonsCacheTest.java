/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.JSONUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author devezhao
 * @since 10/17/2020
 */
class CommonsCacheTest extends TestSupport {

    @Test
    public void test() {
        System.out.println("Cache powered by : " + (Installer.isUseRedis() ? "REDIS" : "EHCACHE"));

        final String key = "abc123";

        // STR
        final String strValue = "1234567890";

        Application.getCommonsCache().put(key, strValue);
        assertEquals(Application.getCommonsCache().get(key), strValue);

        Application.getCommonsCache().evict(key);
        assertNull(Application.getCommonsCache().get(key));

        // MIX
        final JSONObject mixValue = JSONUtils.toJSONObject("abc", "123");

        Application.getCommonsCache().putx(key, mixValue);
        System.out.println(Application.getCommonsCache().get(key));

        Application.getCommonsCache().evict(key);
        assertNull(Application.getCommonsCache().get(key));
    }
}