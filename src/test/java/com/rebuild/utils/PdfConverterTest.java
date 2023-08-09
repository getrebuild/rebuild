/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 */
class PdfConverterTest {

    @Test
    @Disabled
    void convert() throws IOException {
        Path path = ResourceUtils.getFile("C:\\Users\\devezhao\\Desktop\\F分委托货运单-MT4-20230809 (2).xlsx").toPath();
        Path pdf = PdfConverter.convert(path, PdfConverter.TYPE_PDF, Boolean.TRUE);
        System.out.println(path + " > " + pdf);
    }
}