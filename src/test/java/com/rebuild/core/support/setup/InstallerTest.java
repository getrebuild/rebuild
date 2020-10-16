/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * @author devezhao
 * @since 11/25/2019
 */
public class InstallerTest {

    private static final JSONObject USE_H2 = JSON.parseObject(String.format(
            "{installType:99, databaseProps:{dbName:'H2DB-%d'}, systemProps:{appName:'RB999'}, adminProps:{adminPasswd:'admin999'}}",
            System.currentTimeMillis()));

    @Test
    public void getDbInitScript() throws Exception {
        String[] scripts = new Installer(USE_H2).getDbInitScript();
        for (String s : scripts) {
            System.out.println(s);
        }
    }

    @Test
    public void getConnection() throws Exception {
        try (Connection conn = new Installer(USE_H2).getConnection(null)) {
            DatabaseMetaData dmd = conn.getMetaData();
            System.out.println(dmd.getDatabaseProductName() + " " + dmd.getDatabaseProductVersion());
        }
    }

    @Test
    public void install() {
        Installer installer = new Installer(USE_H2);
        installer.installDatabase();
        installer.installAdmin();
    }
}