/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

/**
 */
class OkHttpUtilsTest {

    @Test
    void getAndPost() throws Exception {
        System.out.println(OkHttpUtils.get("https://getrebuild.com/ip"));
        System.out.println(OkHttpUtils.post("https://jsonplaceholder.typicode.com/posts", "Hello! I'm RB!"));
    }
}