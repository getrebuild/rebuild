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

package com.rebuild.server.helper.setup;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSONObject;
import com.mysql.jdbc.Driver;
import com.rebuild.server.RebuildException;
import com.rebuild.server.ServerListener;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.AES;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 系统安装
 *
 * @author devezhao
 * @since 2019/11/25
 */
public class Installer {

    private static final Log LOG = LogFactory.getLog(Installer.class);

    private static final String INSTALL_FILE = ".installed";

    private JSONObject installProps;

    /**
     * @param installProps
     */
    public Installer(JSONObject installProps) {
        this.installProps = installProps;
    }

    /**
     * 执行安装
     */
    public void install() {
        this.installDatabase();
        this.installAdmin();

        // Save .installed
        File dest = SysConfiguration.getFileOfData(INSTALL_FILE);
        Properties dbProps = buildConnectionProps(null);
        dbProps.put("db.passwd.aes", AES.encrypt(dbProps.getProperty("db.passwd")));
        try {
            FileUtils.deleteQuietly(dest);
            try (OutputStream os = new FileOutputStream(dest)) {
                dbProps.store(os, ".installed for REBUILD");
                LOG.warn("Stored install file : " + dest);
            }

        } catch (IOException e) {
            throw new RebuildException(e);
        }

        // init
        new ServerListener().contextInitialized(null);
    }

    /**
     * @param dbName
     * @return
     * @throws SQLException
     */
    public Connection getConnection(String dbName) throws SQLException {
        try {
            Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RebuildException(e);
        }

        Properties props = this.buildConnectionProps(dbName);
        return DriverManager.getConnection(
                props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.passwd"));
    }

    /**
     * @param dbName
     * @return
     */
    protected Properties buildConnectionProps(String dbName) {
        JSONObject dbProps = installProps.getJSONObject("databaseProps");
        String dbUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=false&sessionVariables=default_storage_engine=InnoDB",
                dbProps.getString("dbHost"),
                dbProps.getIntValue("dbPort"),
                StringUtils.defaultIfBlank(dbName, dbProps.getString("dbName")));
        String dbUser = dbProps.getString("dbUser");
        String dbPassword = dbProps.getString("dbPassword");

        // @see jdbc.properties
        Properties props = new Properties();
        props.put("db.url", dbUrl);
        props.put("db.user", dbUser);
        props.put("db.passwd", dbPassword);
        return props;
    }

    /**
     * 数据库
     */
    protected void installDatabase() {
        // 创建数据库
        //noinspection EmptyTryBlock
        try (Connection ignored = getConnection(null)) {
            // NOOP
        } catch (SQLException e) {
            if (!e.getLocalizedMessage().contains("Unknown database")) {
                throw new RebuildException(e);
            }

            // 创建数据库
            String createDb = String.format("CREATE DATABASE `%s` COLLATE utf8mb4_general_ci",
                    installProps.getJSONObject("databaseProps").getString("dbName"));
            try (Connection conn = getConnection("mysql")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(createDb);
                    LOG.warn("Database created : " + createDb);
                }

            } catch (SQLException sqlex) {
                throw new RebuildException(sqlex);
            }
        }

        // 初始化数据库
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                for (String s : getDbInitScript()) {
                    stmt.addBatch(s);
                }

                int[] affected = stmt.executeBatch();
                LOG.warn("Database successed : " + Arrays.toString(affected));
            }

        } catch (SQLException | IOException e) {
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
            // Ignore comments and line of blank
            if (StringUtils.isEmpty(L2) || L2.startsWith("--")) continue;
            if (L2.startsWith("/*") || L2.endsWith("*/")) {
                ignoreMode = L2.startsWith("/*");
                continue;
            } else if (ignoreMode) {
                continue;
            }

            SQL.append(L2);
            if (L2.endsWith(";")) {  // SQL ends
                SQLS.add(SQL.toString());
                SQL = new StringBuilder();
            }
        }
        return SQLS.toArray(new String[0]);
    }

    /**
     * 管理员
     */
    protected void installAdmin() {
        JSONObject adminProps = installProps.getJSONObject("adminProps");
        String adminPasswd = adminProps.getString("adminPasswd");
        String adminMail = adminProps.getString("adminMail");

        List<String> sets = new ArrayList<>();
        if (StringUtils.isNotBlank(adminPasswd)) {
            sets.add(String.format("`PASSWORD` = '%s'", EncryptUtils.toSHA256Hex(adminPasswd)));
        }
        if (StringUtils.isNotBlank(adminMail)) {
            sets.add(String.format("`EMAIL` = '%s'", adminMail));
        }
        if (sets.isEmpty()) return;

        String uql = String.format("update `user` set %s where `LOGIN_NAME` = 'admin'",
                StringUtils.join(sets.iterator(), ", "));
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(uql);
            }

        } catch (SQLException e) {
            LOG.error("Couldn't update `admin` user! Use default.", e);
        }
    }

    // --

    /**
     * 检查安装状态
     *
     * @return
     */
    public static boolean checkInstall() {
        File file = SysConfiguration.getFileOfData(INSTALL_FILE);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                Properties dbProps = new Properties();
                dbProps.load(is);

                String dbPasswd = dbProps.getProperty("db.passwd.aes");
                if (dbPasswd != null) {
                    dbProps.put("db.passwd", AES.decrypt(dbPasswd));
                    dbProps.remove("db.passwd.aes");
                }

                for (Map.Entry<Object, Object> e : dbProps.entrySet()) {
                    System.setProperty((String) e.getKey(), (String) e.getValue());
                }

            } catch (IOException e) {
                throw new RebuildException(e);
            }
            return true;
        }
        return false;
    }
}
