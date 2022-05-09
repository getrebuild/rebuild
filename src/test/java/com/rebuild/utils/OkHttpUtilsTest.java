/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

class OkHttpUtilsTest {

    @Test
    void get() throws Exception {
        System.out.println(OkHttpUtils.get("https://webhook.site/56f4a259-ba64-4f0b-9313-11a64775356a"));
        System.out.println(OkHttpUtils.post("https://webhook.site/56f4a259-ba64-4f0b-9313-11a64775356a", "Hello! I'm RB!"));
    }
}