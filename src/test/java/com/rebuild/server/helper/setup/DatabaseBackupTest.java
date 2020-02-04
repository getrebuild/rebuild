/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
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