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
        Path path = ResourceUtils.getFile("classpath:classification-demo.xlsx").toPath();

        Path pdf = PdfConverter.convert(path, PdfConverter.TYPE_PDF, true);
        System.out.println(path + " > " + pdf);
    }
}