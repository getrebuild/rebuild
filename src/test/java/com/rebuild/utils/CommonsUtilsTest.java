/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 12/17/2020
 */
class CommonsUtilsTest {

    @Test
    void stars() {
        System.out.println(CommonsUtils.stars("abc123"));
        System.out.println(CommonsUtils.stars("15301969039"));
        System.out.println(CommonsUtils.stars("getrebuild@sina.com"));
        System.out.println(CommonsUtils.stars("中文"));

        System.out.println(CommonsUtils.starsPhone("34125678"));
        System.out.println(CommonsUtils.starsPhone("15301969039"));
        System.out.println(CommonsUtils.starsEmail("ge@sina.com"));
        System.out.println(CommonsUtils.starsEmail("getrebuild@sina.com"));
    }
}