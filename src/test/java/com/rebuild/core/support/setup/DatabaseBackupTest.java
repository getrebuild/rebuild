/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.TestSupport;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 02/04/2020
 */
public class DatabaseBackupTest extends TestSupport {

    @Test
    void backup() throws Exception {
        new DatabaseBackup().backup();
    }

    @Test
    void backupFile() throws Exception {
        new DatafileBackup().backup();
    }
}