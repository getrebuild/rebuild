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
import cn.devezhao.commons.sql.builder.UpdateBuilder;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.License;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.cache.EhcacheTemplate;
import com.rebuild.server.helper.cache.JedisCacheTemplate;
import com.rebuild.utils.AES;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 系统安装
 *
 * @author devezhao
 * @since 2019/11/25
 */
public class Installer implements InstallState {

    private static final Log LOG = LogFactory.getLog(Installer.class);

    private JSONObject installProps;
    // 快速安装模式
    final private boolean quickMode;

    /**
     * @param installProps
     */
    public Installer(JSONObject installProps) {
        this.installProps = installProps;
        this.quickMode = installProps.getIntValue("installType") == 99;
    }

    /**
     * 执行安装
     *
     * @throws Exception
     */
    public void install() throws Exception {
        this.installDatabase();
        this.installAdmin();

        // Save install state (file)
        File dest = SysConfiguration.getFileOfData(INSTALL_FILE);
        Properties installProps = buildConnectionProps(null);
        // Redis
        JSONObject cacheProps = this.installProps.getJSONObject("cacheProps");
        if (cacheProps != null && !cacheProps.isEmpty()) {
            installProps.put(ConfigurableItem.CacheHost.name(), cacheProps.getString(ConfigurableItem.CacheHost.name()));
            installProps.put(ConfigurableItem.CachePort.name(), cacheProps.getString(ConfigurableItem.CachePort.name()));
            installProps.put(ConfigurableItem.CachePassword.name(), cacheProps.getString(ConfigurableItem.CachePassword.name()));
        }
        // 加密
        String dbPasswd = (String) installProps.remove("db.passwd");
        installProps.put("db.passwd.aes",
                StringUtils.isBlank(dbPasswd) ? StringUtils.EMPTY : AES.encrypt(dbPasswd));
        String cachePasswd = (String) installProps.remove(ConfigurableItem.CachePassword.name());
        installProps.put(ConfigurableItem.CachePassword.name() + ".aes",
                StringUtils.isBlank(cachePasswd) ? StringUtils.EMPTY : AES.encrypt(cachePasswd));

        try {
            FileUtils.deleteQuietly(dest);
            try (OutputStream os = new FileOutputStream(dest)) {
                installProps.store(os, "INSTALL FILE FOR REBUILD. DON'T DELETE OR MODIFY IT!!!");
                LOG.warn("Stored install file : " + dest);
            }

        } catch (IOException e) {
            throw new SetupException(e);
        }

        // initialize
        try {
            new ServerListener().contextInitialized(null);
        } catch (Exception ex) {
            // If error
            FileUtils.deleteQuietly(dest);
            throw ex;
        }

        // Gen SN
        License.SN();

        // Clean cached
        if (Application.getCommonCache().isUseRedis()) {
            try (Jedis jedis = ((JedisCacheTemplate) Application.getCommonCache().getCacheTemplate()).getJedisPool().getResource()) {
                jedis.flushAll();
            }
        } else {
            ((EhcacheTemplate) Application.getCommonCache().getCacheTemplate()).cache().clear();
        }
    }

    /**
     * @param dbName
     * @return
     * @throws SQLException
     */
    public Connection getConnection(String dbName) throws SQLException {
        Properties props = this.buildConnectionProps(dbName);
        return DriverManager.getConnection(
                props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.passwd"));
    }

    /**
     * @param dbName
     * @return
     */
    private Properties buildConnectionProps(String dbName) {
        final JSONObject dbProps = installProps.getJSONObject("databaseProps");
        if (dbName == null) {
            dbName = dbProps == null ? null : dbProps.getString("dbName");
        }

        if (quickMode) {
            Properties props = new Properties();
            dbName = StringUtils.defaultIfBlank(dbName, "H2DB");
            File dbFile = SysConfiguration.getFileOfData(dbName);
            LOG.warn("Use H2 database : " + dbFile);

            props.put("db.url", String.format("jdbc:h2:file:%s;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE",
                    dbFile.getAbsolutePath()));
            props.put("db.user", "rebuild");
            props.put("db.passwd", "rebuild");
            return props;
        }

        Assert.notNull(dbProps, "[databaseProps] must be null");
        String dbUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=false&sessionVariables=default_storage_engine=InnoDB",
                dbProps.getString("dbHost"),
                dbProps.getIntValue("dbPort"),
                dbName);
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
            int affetced = 0;
            for (String sql : getDbInitScript()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    affetced++;
                }
            }
            LOG.info("Schemes of database created : " + affetced);

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
        boolean ignoreTerms = false;
        for (Object L : LS) {
            String L2 = L.toString().trim();

            // NOTE double 字段也不支持
            boolean H2Unsupported = quickMode
                    && (L2.startsWith("fulltext ") || L2.startsWith("unique ") || L2.startsWith("index "));

            // Ignore comments and line of blank
            if (StringUtils.isEmpty(L2) || L2.startsWith("--") || H2Unsupported) {
                continue;
            }
            if (L2.startsWith("/*") || L2.endsWith("*/")) {
                ignoreTerms = L2.startsWith("/*");
                continue;
            } else if (ignoreTerms) {
                continue;
            }

            SQL.append(L2);
            if (L2.endsWith(";")) {  // SQL ends
                SQLS.add(SQL.toString().replace(",\n)Engine=", "\n)Engine="));
                System.out.println(SQL);
                SQL = new StringBuilder();
            } else {
                SQL.append('\n');
            }
        }
        return SQLS.toArray(new String[0]);
    }

    /**
     * 管理员
     */
    protected void installAdmin() {
        JSONObject adminProps = installProps.getJSONObject("adminProps");
        if (adminProps == null || adminProps.isEmpty()) {
            return;
        }

        String adminPasswd = adminProps.getString("adminPasswd");
        String adminMail = adminProps.getString("adminMail");

        UpdateBuilder ub = SqlBuilder.buildUpdate("user");
        if (StringUtils.isNotBlank(adminPasswd)) {
            ub.addColumn("PASSWORD", EncryptUtils.toSHA256Hex(adminPasswd));
        }
        if (StringUtils.isNotBlank(adminMail)) {
            ub.addColumn("EMAIL", adminMail);
        }
        if (!ub.hasColumn()) {
            return;
        }

        ub.setWhere("LOGIN_NAME = 'admin'");
        executeSql(ub.toSql());
    }

    /**
     * @param sql
     */
    private void executeSql(String sql) {
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException sqlex) {
            LOG.error("Couldn't execute SQL : " + sql, sqlex);
        }
    }
}
