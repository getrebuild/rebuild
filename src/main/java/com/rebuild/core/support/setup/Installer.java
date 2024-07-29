/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.sql.SqlBuilder;
import cn.devezhao.commons.sql.builder.UpdateBuilder;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.BootConfiguration;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.cache.BaseCacheTemplate;
import com.rebuild.core.cache.RedisDriver;
import com.rebuild.core.configuration.NavBuilder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.rbstore.BusinessModelImporter;
import com.rebuild.core.rbstore.ClassificationImporter;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.UseRedis;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.AES;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.args.FlushMode;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import static com.rebuild.core.support.ConfigurationItem.CacheHost;
import static com.rebuild.core.support.ConfigurationItem.CachePassword;
import static com.rebuild.core.support.ConfigurationItem.CachePort;

/**
 * 系统安装
 *
 * @author devezhao
 * @since 2019/11/25
 */
@Slf4j
public class Installer implements InstallState {

    public static final String CONF_PREFIX = "db.";

    // 快速安装模式（H2 数据库）
    private boolean quickMode;

    private JSONObject installProps;

    private DbInfo dbInfo;

    // for MySQL8
    private boolean allowPublicKeyRetrieval = false;

    private String EXISTS_SN;

    private Installer() { }

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
    public void install() throws Exception {
        final boolean dbNew = this.installDatabase();

        this.installAdmin();

        Properties installProps = buildConnectionProps(null);

        String dbPasswd = (String) installProps.remove("db.passwd");
        if (StringUtils.isNotBlank(dbPasswd)) {
            installProps.put("db.passwd", String.format("AES(%s)", AES.encrypt(dbPasswd)));
        }

        if (dbInfo.isOceanBase()) {
            installProps.put("db.type", "OceanBase");
        } else if (dbInfo.isMySQL80()) {
            // https://www.cnblogs.com/lusaisai/p/13372763.html
            String dbUrl8 = installProps.getProperty("db.url");
            if (!dbUrl8.contains("allowPublicKeyRetrieval")) dbUrl8 += "&allowPublicKeyRetrieval=true";
            installProps.put("db.url", dbUrl8);
        }

        // Redis
        JSONObject cacheProps = this.installProps.getJSONObject("cacheProps");
        if (cacheProps != null && !cacheProps.isEmpty()) {
            installProps.put(CONF_PREFIX + CacheHost.name(), cacheProps.getString(CacheHost.name()));
            installProps.put(CONF_PREFIX + CachePort.name(), cacheProps.getString(CachePort.name()));

            String cachePasswd = cacheProps.getString(CachePassword.name());
            if (StringUtils.isNotBlank(cachePasswd)) {
                installProps.put(CONF_PREFIX + CachePassword.name(), String.format("AES(%s)", AES.encrypt(cachePasswd)));
            }
        } else {
            // Use ehcache
            installProps.put(CONF_PREFIX + CacheHost.name(), "0");
        }

        // Save install state (file)
        File dest = RebuildConfiguration.getFileOfData(INSTALL_FILE);
        try {
            FileUtils.deleteQuietly(dest);
            try (OutputStream os = Files.newOutputStream(dest.toPath())) {
                installProps.store(os, "REBUILD INSTALLER MAGIC !!! DO NOT EDIT !!!");
                log.info("Saved installation file : {}", dest);
            }

        } catch (IOException e) {
            throw new SetupException(e);
        }

        // Refresh
        try {
            refresh();
        } catch (Exception ex) {
            FileUtils.deleteQuietly(dest);
            throw ex;
        }

        // Clean cached
        clearAllCache();

        if (!dbNew) return;

        // 附加数据

        try {
            // 导入实体
            String[] created = this.installModel();
            // 初始化菜单
            if (created.length > 0) NavBuilder.instance.addInitNavOnInstall(created);

        } catch (Exception ex) {
            log.error("Error installing business module", ex);
        }

        try {
            this.installClassificationAsync();
        } catch (Exception ex) {
            log.error("Error installing classification data", ex);
        }
    }

    /**
     * 刷新配置
     */
    private void refresh() throws Exception {
        // 重配置
        Application.getBean(BootEnvironmentPostProcessor.class).postProcessEnvironment(null, null);

        // 刷新: 数据源
        DruidDataSource ds = (DruidDataSource) Application.getBean(DataSource.class);
        ds.restart();
        ds.setUrl(BootEnvironmentPostProcessor.getProperty("db.url"));
        ds.setUsername(BootEnvironmentPostProcessor.getProperty("db.user"));
        ds.setPassword(BootEnvironmentPostProcessor.getProperty("db.passwd"));

        // 刷新: REDIS
        JedisPool pool = BootConfiguration.createJedisPoolInternal();
        for (Object o : Application.getContext().getBeansOfType(UseRedis.class).values()) {
            if (!((BaseCacheTemplate<?>) o).reinjectJedisPool(pool)) break;
        }

        String inputSN = System.getProperty("SN");
        if (StringUtils.isNotBlank(inputSN)) {
            RebuildConfiguration.set(ConfigurationItem.SN, inputSN);
            EXISTS_SN = inputSN;
        }

        // L
        if (EXISTS_SN != null) {
            System.setProperty("SN", EXISTS_SN);
            License.siteApiNoCache("api/authority/query");
        }

        // 初始化
        Application.init();

        System.setProperty("SN", StringUtils.EMPTY);
        log.info("Installed SN : {}", License.SN());
    }

    /**
     * @param dbName
     * @return
     * @throws SQLException
     */
    public Connection getConnection(String dbName) throws SQLException {
        Properties props = buildConnectionProps(dbName);
        try {
            return DriverManager.getConnection(
                    props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.passwd"));
        } catch (SQLException ex) {

            // Plugin 'mysql_native_password' is not loaded
            allowPublicKeyRetrieval = ex.getMessage().contains("Public Key Retrieval is not allowed");
            if (allowPublicKeyRetrieval) {
                props = buildConnectionProps(dbName);
                return DriverManager.getConnection(
                        props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.passwd"));
            }

            throw ex;
        }
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
            File dbFile = RebuildConfiguration.getFileOfData(dbName);
            log.warn("Use H2 database : {}", dbFile);

            props.put("db.url", String.format("jdbc:h2:file:%s;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE",
                    dbFile.getAbsolutePath()));
            props.put("db.user", "rebuild");
            props.put("db.passwd", "rebuild");
            return props;
        }

        Assert.notNull(dbProps, "[databaseProps] cannot be null");

        String dbUrl = String.format(
                "jdbc:mysql://%s:%d/%s?characterEncoding=UTF8&useUnicode=true&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=%s",
                dbProps.getString("dbHost"),
                dbProps.getIntValue("dbPort"),
                dbName,
                getTimeZoneId());

        // https://www.cnblogs.com/lusaisai/p/13372763.html
        if (allowPublicKeyRetrieval) dbUrl += "&allowPublicKeyRetrieval=true";

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
     *
     * @return
     */
    protected boolean installDatabase() {
        dbInfo = getDbInfo();

        // 本身就是 RB 数据库，无需创建
        if (isRbDatabase()) {
            log.warn("Use exists REBUILD database without create");
            return false;
        }

        if (!quickMode) {
            // 创建数据库（如果需要）
            // noinspection EmptyTryBlock
            try (Connection ignored = getConnection(null)) {
                // NOOP
            } catch (SQLException e) {
                if (!e.getMessage().contains("Unknown database")) {
                    throw new SetupException(e);
                }

                // 创建
                String createDb = String.format("CREATE DATABASE `%s` COLLATE utf8mb4_general_ci",
                        installProps.getJSONObject("databaseProps").getString("dbName"));
                try (Connection conn = getConnection("mysql")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createDb);
                        log.warn("Database created : {}", createDb);
                    }

                } catch (SQLException sqlex) {
                    throw new SetupException(sqlex);
                }
            }
        }

        // 初始化数据库
        try (Connection conn = getConnection(null)) {
            int affetced = 0;
            for (final String s : getDbInitScript()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(s);
                    affetced++;
                }
            }
            log.info("Database schemes init : {}", affetced);

        } catch (SQLException | IOException e) {
            throw new SetupException(e);
        }

        return true;
    }

    /**
     * @return
     */
    public DbInfo getDbInfo() {
        if (quickMode) return new DbInfo("H2");

        try (Connection conn = getConnection("mysql")) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select version()")) {
                    if (rs.next()) return new DbInfo(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            log.warn("Cannot get database info", ex);
        }
        return new DbInfo("MySQL");
    }

    /**
     * @return
     * @throws IOException
     */
    protected String[] getDbInitScript() throws IOException {
        List<String> LS;
        try (InputStream is = CommonsUtils.getStreamOfRes("scripts/db-init.sql")) {
            LS = IOUtils.readLines(is, AppUtils.UTF8);
        }

        List<String> SQLS = new ArrayList<>();
        StringBuilder SQL = new StringBuilder();
        boolean ignoreTerms = false;
        for (Object L : LS) {
            String L2 = L.toString().trim();
            // IGNORED
            if (StringUtils.isEmpty(L2) || L2.startsWith("--")
                    || (dbInfo != null && dbInfo.isIgnoredSqlLine(L2))) {
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

        ub.setWhere(String.format("USER_ID = '%s'", UserService.ADMIN_USER));
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
            log.error("Cannot execute SQL : {}", sql, sqlex);
        }
    }

    /**
     * 初始业务实体
     *
     * @return
     */
    protected String[] installModel() {
        JSONArray modelProps = installProps.getJSONArray("modelProps");
        if (modelProps == null || modelProps.isEmpty()) {
            return new String[0];
        }

        BusinessModelImporter bmi = new BusinessModelImporter();

        Map<String, String> allModels = new HashMap<>();
        for (Object model : modelProps) {
            allModels.putAll(bmi.findRefs((String) model));
        }

        bmi.setModelFiles(allModels.values().toArray(new String[0]));
        bmi.setUser(UserService.SYSTEM_USER);
        TaskExecutors.run(bmi);

        Map<String, Integer> sorted = new HashMap<>();
        int seq = 0;
        for (Object o : bmi.getIndexSchemas()) {
            sorted.put(((JSONObject) o).getString("key"), seq++);
        }

        List<String> created = bmi.getCreatedEntity();
        created.sort((o1, o2) -> {
            int i1 = sorted.getOrDefault(o1, 99);
            int i2 = sorted.getOrDefault(o2, 99);
            return Integer.compare(i1, i2);
        });
        return created.toArray(new String[0]);
    }

    /**
     * 分类数据（异步）
     */
    protected void installClassificationAsync() {
        String[][] init = new String[][] {
                new String[] { "018-0000000000000001", "CHINA-PCAS.json" },
                new String[] { "018-0000000000000002", "CHINA-ICNEA.json" },
        };

        for (String[] i : init) {
            ClassificationImporter c = new ClassificationImporter(ID.valueOf(i[0]), i[1]);
            TaskExecutors.submit(c, UserService.SYSTEM_USER);
        }
    }

    /**
     * 是否为 RB 数据库，系统检测 `system_config` 表
     *
     * @return
     */
    public boolean isRbDatabase() {
        String rbSql = SqlBuilder.buildSelect("system_config")
                .addColumns("ITEM", "VALUE")
                .setWhere("ITEM = 'DBVer' or ITEM = 'SN'")
                .toSql();

        EXISTS_SN = null;
        try (Connection conn = getConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(rbSql)) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String value = rs.getString(2);
                        if ("DBVer".equalsIgnoreCase(name)) {
                            log.info("Check exists REBUILD database version : {}", value);
                        } else if ("SN".equalsIgnoreCase(name)) {
                            EXISTS_SN = value;
                        }
                    }

                    return true;
                }
            }

        } catch (SQLException ex) {
            log.info("Check REBUILD database error : {}", ex.getLocalizedMessage());
        }
        return false;
    }

    protected String getTimeZoneId() {
        String tz = TimeZone.getDefault().getID();
        if (StringUtils.isBlank(tz)) {
            try {
                //noinspection ResultOfMethodCallIgnored
                ZoneId.of("GMT+08:00");
                tz = "GMT+08:00";
            } catch (DateTimeException unsupportZoneId) {
                tz = "Asia/Shanghai";
            }

            log.warn("No time-zone specified! Use default : {}", tz);
        }

        // 转义
        if (tz.contains(" ")) tz = tz.replace(" ", "%2B");
        if (tz.contains("+")) tz = tz.replace("+", "%2B");

        return tz;
    }

    // --

    /**
     * 是否 H2 数据库
     *
     * @return
     */
    public static boolean isUseH2() {
        String dbUrl = BootEnvironmentPostProcessor.getProperty("db.url");
        return dbUrl != null && dbUrl.startsWith("jdbc:h2:");
    }

    /**
     * 是否 OceanBase 数据库
     *
     * @return
     */
    public static boolean isUseOceanBase() {
        String dbType = BootEnvironmentPostProcessor.getProperty("db.type");
        return dbType != null && dbType.contains("OceanBase");
    }

    /**
     * @return
     */
    public static boolean isUseRedis() {
        return Application.getCommonsCache().getCacheTemplate() instanceof RedisDriver;
    }

    /**
     * 是否已安装
     *
     * @return
     */
    public static boolean isInstalled() {
        return new Installer().checkInstalled();
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        if (isUseRedis()) {
            try (Jedis jedis = Application.getCommonsCache().getJedisPool().getResource()) {
                // https://redis.io/commands/flushdb/
                try {
                    jedis.flushDB(FlushMode.SYNC);  // v6.2.0
                } catch (Exception v620) {
                    jedis.flushDB();
                }
            }
        } else {
            Application.getCommonsCache().getEhcacheCache().clear();
        }
    }
}