/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 */
class CommonsUtilsTest {

    @Test
    void parseDate() {
        assertNotNull(CommonsUtils.parseDate("2020-01-01"));
        assertNotNull(CommonsUtils.parseDate("2020-01"));
        assertNotNull(CommonsUtils.parseDate("2020"));
        assertNotNull(CommonsUtils.parseDate("2020年01月01日"));
        assertNotNull(CommonsUtils.parseDate("2020年01月"));
        assertNotNull(CommonsUtils.parseDate("2020年"));
        assertNotNull(CommonsUtils.parseDate("2020年01月01日 23:23:23.888"));
        assertNotNull(CommonsUtils.parseDate("2020年01月01日 23:23:23"));
        assertNotNull(CommonsUtils.parseDate("2020年01月01日 23:23"));
        assertNotNull(CommonsUtils.parseDate("2020年01月01日 23"));
    }
}