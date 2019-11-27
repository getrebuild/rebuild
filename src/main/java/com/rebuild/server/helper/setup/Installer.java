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
import cn.devezhao.commons.sql.SqlBuilder;
import cn.devezhao.commons.sql.builder.InsertBuilder;
import cn.devezhao.commons.sql.builder.UpdateBuilder;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.mysql.jdbc.Driver;
import com.rebuild.server.ServerListener;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.Lisence;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.AES;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final String INSTALL_FILE = ".rebuild";

    private JSONObject installProps;
    // 快速安装模式
    private boolean quickMode;

    /**
     * @param installProps
     */
    public Installer(JSONObject installProps) {
        this.installProps = installProps;
        this.quickMode = installProps.getIntValue("installType") == 99;
    }

    /**
     * 执行安装
     */
    public void install() {
        this.installDatabase();
        this.installSystem();
        this.installAdmin();

        // Save install state
        File dest = SysConfiguration.getFileOfData(INSTALL_FILE);
        Properties dbProps = buildConnectionProps(null);
        dbProps.put("db.passwd.aes", AES.encrypt((String) dbProps.remove("db.passwd")));
        try {
            FileUtils.deleteQuietly(dest);
            try (OutputStream os = new FileOutputStream(dest)) {
                dbProps.store(os, "INSTALL FILE FOR REBUILD. DON'T DELETE OR MODIFY IT!!!");
                LOG.warn("Stored install file : " + dest);
            }

        } catch (IOException e) {
            throw new SetupException(e);
        }

        // init again
        new ServerListener().contextInitialized(null);
        // Gen SN
        Lisence.SN();
    }

    /**
     * @param dbName
     * @return
     * @throws SQLException
     */
    public Connection getConnection(String dbName) throws SQLException {
        try {
            Class.forName(quickMode ? org.h2.Driver.class.getName() : Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new SetupException(e);
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
        if (quickMode) {
            Properties props = new Properties();
            File dbFile = SysConfiguration.getFileOfData("H2DB");
            if (dbFile.exists()) FileUtils.deleteQuietly(dbFile);
            props.put("db.url",
                    String.format("jdbc:h2:file:%s;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", dbFile.getAbsolutePath()));
            props.put("db.user", "sa");
            props.put("db.passwd", "sa");
            return props;
        }

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
        if (!quickMode) {

            // 创建数据库（如果需要）
            // noinspection EmptyTryBlock
            try (Connection ignored = getConnection(null)) {
                // NOOP
            } catch (SQLException e) {
                if (!e.getLocalizedMessage().contains("Unknown database")) {
                    throw new SetupException(e);
                }

                // 创建
                String createDb = String.format("CREATE DATABASE `%s` COLLATE utf8mb4_general_ci",
                        installProps.getJSONObject("databaseProps").getString("dbName"));
                try (Connection conn = getConnection("mysql")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createDb);
                        LOG.warn("Database created : " + createDb);
                    }

                } catch (SQLException sqlex) {
                    throw new SetupException(sqlex);
                }
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
            throw new SetupException(e);
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
            String L2 = L.toString().trim();

            // Ignore comments and line of blank
            if (StringUtils.isEmpty(L2) || L2.startsWith("--")) continue;
            if (L2.startsWith("/*") || L2.endsWith("*/")) {
                ignoreMode = L2.startsWith("/*");
                continue;
            } else if (ignoreMode) {
                continue;
            }

            SQL.append(L2).append('\n');
            if (L2.endsWith(";")) {  // SQL ends
                SQLS.add(SQL.toString());
                SQL = new StringBuilder();
            }
        }
        return SQLS.toArray(new String[0]);
    }

    /**
     * 系统参数
     */
    protected void installSystem() {
        JSONObject systemProps = installProps.getJSONObject("systemProps");
        if (systemProps == null || systemProps.isEmpty()) return;

        insertSystemProp(ConfigurableItem.DataDirectory, systemProps.getString("dataDirectory"));
        insertSystemProp(ConfigurableItem.AppName, systemProps.getString("appName"));
        insertSystemProp(ConfigurableItem.HomeURL, systemProps.getString("homeUrl"));
    }

    /**
     * 管理员
     */
    protected void installAdmin() {
        JSONObject adminProps = installProps.getJSONObject("adminProps");
        if (adminProps == null || adminProps.isEmpty()) return;

        String adminPasswd = adminProps.getString("adminPasswd");
        String adminMail = adminProps.getString("adminMail");

        UpdateBuilder ub = SqlBuilder.buildUpdate("user");
        if (StringUtils.isNotBlank(adminPasswd)) {
            ub.addColumn("PASSWORD", EncryptUtils.toSHA256Hex(adminPasswd));
        }
        if (StringUtils.isNotBlank(adminMail)) {
            ub.addColumn("EMAIL", adminMail);
        }
        if (!ub.hasColumn()) return;

        ub.setWhere("LOGIN_NAME = 'admin'");
        executeSql(ub.toSql());
    }

    private void insertSystemProp(ConfigurableItem item, String value) {
        if (StringUtils.isBlank(value)) return;

        InsertBuilder ib = SqlBuilder.buildInsert("system_config")
                .addColumn("CONFIG_ID", ID.newId(EntityHelper.SystemConfig))
                .addColumn("ITEM", item.name())
                .addColumn("VALUE", value);
        executeSql(ib.toSql());
    }

    private void executeSql(String sql) {
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException sqlex) {
            LOG.error("Couldn't execute SQL : " + sql, sqlex);
        }
    }

    // --

    /**
     * 检查安装状态
     *
     * @return
     */
    public static boolean checkInstall() {
//        if (Application.devMode()) return true;  // for dev

        File file = SysConfiguration.getFileOfData(INSTALL_FILE);
        if (file.exists()) {
            try {
                Properties dbProps = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
                String dbPasswd = (String) dbProps.remove("db.passwd.aes");
                if (dbPasswd != null) {
                    dbProps.put("db.passwd", AES.decrypt(dbPasswd));
                }

                for (Map.Entry<Object, Object> e : dbProps.entrySet()) {
                    System.setProperty((String) e.getKey(), (String) e.getValue());
                    if (e.getKey().equals("db.url") && ((String) e.getValue()).contains("jdbc:h2:")) {
                        LOG.warn("Using QuickMode with H2 database!");
                        try {
                            Class.forName(org.h2.Driver.class.getName());
                        } catch (ClassNotFoundException h2ex) {
                            throw new SetupException(h2ex);
                        }
                    }
                }
            } catch (IOException e) {
                throw new SetupException(e);
            }

            return true;
        }
        return false;
    }
}
