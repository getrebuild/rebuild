/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

/**
 */
public class Tests {

    @Test
    void test() {
        System.out.println(ZoneId.of("GMT+8"));
    }
}
