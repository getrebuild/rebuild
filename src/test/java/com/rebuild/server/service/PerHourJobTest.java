/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service;

import com.rebuild.server.TestSupport;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author devezhao
 * @since 2020/2/25
 */
public class PerHourJobTest extends TestSupport {

    @Test
    public void doDatabaseBackup() {
        // @see DatabaseBackupTest
    }

    @Test
    public void doCleanTempFiles() {
        new PerHourJob().doCleanTempFiles();
    }
}