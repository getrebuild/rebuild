/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 */
class CompressUtilsTest {

    @Test
    void zip() throws IOException {
        CompressUtils.forceZip(
                new File("D:\\GitHub\\rebuild\\rebuild"),
                new File("D:\\GitHub\\rebuild.zip"),
                pathname -> !pathname.getName().contains("node_modules")
        );
    }
}