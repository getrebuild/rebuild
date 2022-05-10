/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/12/22
 */
class DataDesensitizedTest {

    @Test
    void masking() {
        System.out.println(DataDesensitized.any("abc123"));
        System.out.println(DataDesensitized.any("15301969039"));
        System.out.println(DataDesensitized.any("getrebuild@sina.com"));
        System.out.println(DataDesensitized.any("中文"));

        System.out.println(DataDesensitized.phone("34125678"));
        System.out.println(DataDesensitized.phone("15301969039"));
        System.out.println(DataDesensitized.email("ge@sina.com"));
        System.out.println(DataDesensitized.email("getrebuild@sina.com"));
    }

}