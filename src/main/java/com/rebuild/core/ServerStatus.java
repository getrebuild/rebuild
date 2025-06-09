/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.sql.SqlBuilder;
import cn.devezhao.persist4j.util.SqlHelper;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.Installer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 服务状态检查/监控
 *
 * @author devezhao
 * @since 10/31/2018
 */
@Slf4j
public final class ServerStatus {

    // 启动时间
    public static final Date STARTUP_TIME = CalendarUtils.now();
    // 启动实例ID
    public static final String STARTUP_ONCE = genStartupOnce();

    private static long LastCheckTime = 0;
    private static final List<Status> LAST_STATUS = new ArrayList<>();

    /**
     * @param realtime
     * @return
     */
    public static List<Status> getLastStatus(boolean realtime) {
        // 30 秒缓存
        if (realtime || System.currentTimeMillis() - LastCheckTime > 30 * 1000) {
            checkAll();
        }

        synchronized (LAST_STATUS) {
            return Collections.unmodifiableList(LAST_STATUS);
        }
    }

    /**
     * @return
     */
    public static boolean isStatusOK() {
        for (Status s : getLastStatus(false)) {
            if (!s.success) return false;
        }
        return true;
    }

    /**
     * @return
     */
    static boolean checkAll() {
        List<Status> last = new ArrayList<>();
        last.add(checkCreateFile());
        last.add(checkDatabase());
        last.add(checkCacheService());

        synchronized (LAST_STATUS) {
            LAST_STATUS.clear();
            LAST_STATUS.addAll(last);
        }
        LastCheckTime = System.currentTimeMillis();
        return isStatusOK();
    }

    /**
     * @return
     */
    static Status checkDatabase() {
        String name = "Database";
        if (Installer.isUseH2()) name += "/H2";
        else name += "/MYSQL";

        final String url = BootEnvironmentPostProcessor.getProperty("db.url");
        final String rbSql = SqlBuilder.buildSelect("system_config")
                .addColumns("ITEM", "VALUE")
                .setWhere("ITEM = 'DBVer' or ITEM = 'SN'")
                .toSql();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(
                    url,
                    BootEnvironmentPostProcessor.getProperty("db.user"),
                    BootEnvironmentPostProcessor.getProperty("db.passwd"));
            stmt = conn.createStatement();
            rs = stmt.executeQuery(rbSql);
            if (rs.next()) {
                log.debug("Check database success : {}", rs.getString(1));
            }

        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        } finally {
            SqlHelper.close(rs);
            SqlHelper.close(stmt);
            //noinspection deprecation
            SqlHelper.close(conn);
        }
        return Status.success(name);
    }

    /**
     * @return
     */
    static Status checkCreateFile() {
        String name = "CreateFile";
        File test = null;
        RandomAccessFile raf = null;
        try {
            test = new File(FileUtils.getTempDirectory(), "ServerStatus.test");
            raf = new RandomAccessFile(test, "rw");
            raf.setLength(1024 * 1024 * 5);  // 5M

            if (!test.exists()) {
                return Status.error(name, "Cannot create file in temp-directory");
            }

        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        } finally {
            if (raf != null) IOUtils.closeQuietly(raf);
            if (test != null) FileUtils.deleteQuietly(test);
        }
        return Status.success(name);
    }

    /**
     * @return
     */
    static Status checkCacheService() {
        CommonsCache cache = Application.getCommonsCache();
        String name = "Cache";
        if (Installer.isUseRedis()) {
            name += "/REDIS";
        } else {
            name += "/EHCACHE";

            // fix:异常关闭文件损坏
            try {
                cache.getx("ServerStatus.test");
            } catch (Exception ex) {
                log.warn("Clear/Fixs ehcache because : {}", ex.getLocalizedMessage());
                Installer.clearAllCache();
            }
        }

        try {
            cache.putx("ServerStatus.test", 1, 60);
            cache.getx("ServerStatus.test");
        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
        return Status.success(name);
    }

    private static String genStartupOnce() {
        File once = RebuildConfiguration.getFileOfTemp(".startup");
        String onceToken;
        try {
            if (once.exists()) {
                onceToken = FileUtils.readFileToString(once, StandardCharsets.UTF_8);
            } else {
                onceToken = CodecUtils.randomCode(32);
                FileUtils.writeStringToFile(once, onceToken, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            onceToken = CodecUtils.randomCode(32);
        }
        // 32+8
        return onceToken + CodecUtils.randomCode(8);
    }

    // 状态
    public static class Status {
        final public String name;
        final public boolean success;
        final public String error;

        @Override
        public String toString() {
            if (success) {
                return String.format("%s : [ OK ]", name);
            } else {
                return String.format("%s : [ ERROR ] %s", name, error);
            }
        }

        private Status(String name, boolean success, String error) {
            this.name = name;
            this.success = success;
            this.error = error;

            if (success) {
                log.debug("Checking {}", this);
            } else {
                log.error("Checking {}", this);
            }
        }

        private static Status success(String name) {
            return new Status(name, true, null);
        }

        private static Status error(String name, String error) {
            return new Status(name, false, StringUtils.defaultIfBlank(error, "ERROR"));
        }
    }
}
