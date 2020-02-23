/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/2/4
 */
public class JSONUtilsTest {

    @Test
    public void toJSONObjectArray() {
        String[] keys = new String[]{"a", "b", "c"};
        Object[][] values = new Object[][]{
                new Object[]{1, 2, 3},
                new Object[]{11, 22, 33},
                new Object[]{111, 222, 333}
        };
        System.out.println(JSONUtils.toJSONObjectArray(keys, values));
    }

    @Test
    public void clone1() {
        JSONUtils.clone(JSON.parseObject("{a:1,b:2}"));
    }
}