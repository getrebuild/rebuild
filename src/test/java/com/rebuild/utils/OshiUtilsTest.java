/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 */
class OshiUtilsTest {

    @Test
    void getSI() {
        System.out.println(OshiUtils.getSI());
    }

    @Test
    void getOsMemoryUsed() {
        System.out.println(Arrays.toString(OshiUtils.getOsMemoryUsed()));
    }

    @Test
    void getSystemLoad() {
        System.out.println(OshiUtils.getSystemLoad());
    }

    @Test
    void getLocalIp() {
        System.out.println(OshiUtils.getLocalIp());
    }
}