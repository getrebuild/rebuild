/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.setup;

import com.rebuild.server.TestSupport;
import org.junit.Test;

/**
 * @author devezhao
 * @since 02/04/2020
 */
public class DatabaseBackupTest extends TestSupport {

    @Test
    public void backup() throws Exception {
        new DatabaseBackup().backup();
    }
}