/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author RB
 * @since 2022/6/8
 */
@Slf4j
class SysbaseSupportTest {

    @Test
    void getLogbackFile() throws IOException {
        log.info("SysbaseSupportTest#getLogbackFile");

        File file = SysbaseSupport.getLogbackFile();
        System.out.println(file + " >> " + file.exists());
        System.out.println(FileUtils.readFileToString(file));
    }
}