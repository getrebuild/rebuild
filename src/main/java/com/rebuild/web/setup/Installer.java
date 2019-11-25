/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.setup;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.utils.AES;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 系统安装
 *
 * @author devezhao
 * @since 2019/11/25
 */
public class Installer {

    private static final Log LOG = LogFactory.getLog(Installer.class);

    private JSONObject installProps;

    protected Installer(JSONObject installProps) {
        this.installProps = installProps;
    }

    /**
     * 执行安装
     */
    public void install() {
        this.installDatabase();
    }

    /**
     * @param dbProps
     * @param aes
     * @return
     */
    protected Properties buildConnectionProps(JSONObject dbProps, boolean aes) {
        dbProps = dbProps == null ? installProps.getJSONObject("databaseProps") : dbProps;

        String dbUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=false&sessionVariables=default_storage_engine=InnoDB",
                dbProps.getString("dbHost"),
                dbProps.getIntValue("dbPort"),
                dbProps.getString("dbName"));
        String dbUser = dbProps.getString("dbUser");
        String dbPassword = dbProps.getString("dbPassword");

        // @see jdbc.properties
        Properties props = new Properties();
        props.put("db.url", dbUrl);
        props.put("db.user", dbUser);
        if (aes) {
            props.put("db.passwd.aes", AES.encrypt(dbPassword));
        } else {
            props.put("db.passwd", dbPassword);
        }
        return props;
    }

    /**
     */
    protected void installDatabase() {
        Properties dbProps = this.buildConnectionProps(null, Boolean.FALSE);
        try (Connection conn = DriverManager.getConnection(
                dbProps.getProperty("db.url"), dbProps.getProperty("db.user"), dbProps.getProperty("db.passwd"))) {

//            for (String SQL : getDbInitScript()) {
//                try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
//                    System.out.println(SQL);
//                    stmt.executeUpdate();
//                    SqlHelper.clear(stmt);
//                }
//            }

            try (Statement stmt = conn.createStatement()) {
                for (String s : getDbInitScript()) {
                    stmt.addBatch(s);
                }

                int[] affected = stmt.executeBatch();
                LOG.warn("Database successed : " + StringUtils.join(Arrays.asList(affected), ", "));
            }

        } catch (SQLException | IOException e) {
            LOG.error(null, e);
            throw new RebuildException(e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    protected String[] getDbInitScript() throws IOException {
        File script = ResourceUtils.getFile("classpath:scripts/db-init.sql");
        List<?> LS = FileUtils.readLines(script, "utf-8");

        List<String> SQLS = new ArrayList<>();
        StringBuilder SQL = new StringBuilder();
        boolean ignoreMode = false;
        for (Object L : LS) {
            String L2 = L.toString();
            if (StringUtils.isEmpty(L2) || L2.startsWith("--")) continue;
            if (L2.startsWith("/*") || L2.endsWith("*/")) {
                ignoreMode = L2.startsWith("/*");
                continue;
            } else if (ignoreMode) {
                continue;
            }

            SQL.append(L2);
            if (L2.endsWith(";")) {
                SQLS.add(SQL.toString());
                SQL = new StringBuilder();
            }
        }
        return SQLS.toArray(new String[0]);
    }

    /**
     */
    protected void installSystems() {
    }

    /**
     */
    protected void installAdmin() {
    }
}
