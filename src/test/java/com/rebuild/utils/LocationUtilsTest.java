/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

class LocationUtilsTest {

    @Test
    void getLocation() {
        System.out.println(LocationUtils.getLocation("8.8.8.8"));
        System.out.println(LocationUtils.getLocation("59.82.84.31"));
        System.out.println(LocationUtils.getLocation("192.168.1.1"));
        System.out.println(LocationUtils.getLocation("123"));
    }
}