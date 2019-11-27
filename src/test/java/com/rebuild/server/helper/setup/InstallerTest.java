/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.setup;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * @author devezhao
 * @since 11/25/2019
 */
public class InstallerTest {

    private static final JSONObject USE_H2 = JSONUtils.toJSONObject("installType", 99);

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
    public void install() throws Exception {
        Installer installer = new Installer(USE_H2);
        installer.installDatabase();
        installer.installSystem();
        installer.installAdmin();
    }
}