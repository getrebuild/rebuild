/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 */
class CommonsUtilsTest {

    @Test
    void parseDate() {
        parseDateWithPrint("2025年09月25日 (周四) 00:55:00");
        parseDateWithPrint("2025年09月25日 (周四)");

        parseDateWithPrint("2025-2-21");
        parseDateWithPrint("2025年2月2日");
        parseDateWithPrint("2025年2月21日");
        parseDateWithPrint("2025/2/2");
        parseDateWithPrint("2025/2/21");

        parseDateWithPrint("2020-01");
        parseDateWithPrint("2020");
        parseDateWithPrint("2020年01月01日");
        parseDateWithPrint("2020年01月");
        parseDateWithPrint("2020年");
        parseDateWithPrint("2020年01月01日 23:23:23.888");
        parseDateWithPrint("2020年01月01日 23:23:23");
        parseDateWithPrint("2020年01月01日 23:23");
        parseDateWithPrint("2020年01月01日 23");
    }

    void parseDateWithPrint(String source) {
        Date d = CommonsUtils.parseDate(source);
        System.out.println(source + " > " + (d == null ? "null" : CalendarUtils.getUTCDateTimeFormat().format(d)));
        assertNotNull(d);
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