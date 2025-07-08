/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

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

    @Test
    void testBase64() throws Exception {
        File file = new ClassPathResource("barcode.png").getFile();
        String base64String = CommonsUtils.fileToBase64(file);
        System.out.println("base64String : " + base64String);

        CommonsUtils.base64ToFile(base64String, new File(FileUtils.getTempDirectory(), "barcode.png"));
    }

    @Test
    void sanitizeHtml() {
        System.out.println(CommonsUtils.sanitizeHtml("<scriPT>alert(1)</script>"));
    }
}