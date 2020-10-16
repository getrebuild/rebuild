/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.TestSupport;
import com.rebuild.core.support.RebuildConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * @author devezhao
 * @since 02/04/2020
 */
public class DatabaseBackupTest extends TestSupport {

    @Test
    public void backup() throws Exception {
        new DatabaseBackup().backup();
    }

    @Test
    public void zip() throws Exception {
        File file = ResourceUtils.getFile("classpath:metadata-conf.xml");
        File dest = RebuildConfiguration.getFileOfTemp(file.getName() + ".zip");
        DatabaseBackup.zip(file, dest);
        System.out.println("Zip to : " + dest);
    }
}