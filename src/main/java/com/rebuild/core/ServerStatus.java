/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.runtime.MemoryInformationBean;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.setup.Installer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
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

    /**
     * 启动时间
     */
    public static final Date STARTUP_TIME = CalendarUtils.now();
    /**
     * 启动实例标识
     */
    public static final String STARTUP_ONCE = CodecUtils.randomCode(40);

    private static long LastCheckTime = 0;
    private static final List<Status> LAST_STATUS = new ArrayList<>();

    /**
     * @param realtime
     * @return
     */
    public static List<Status> getLastStatus(boolean realtime) {
        // 60 秒缓存
        if (realtime || System.currentTimeMillis() - LastCheckTime > 60 * 1000) {
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
        if (Installer.isUseH2()) return Status.success(name + "/H2");

        String url = BootEnvironmentPostProcessor.getProperty("db.url");
        try {
            Connection c = DriverManager.getConnection(
                    url,
                    BootEnvironmentPostProcessor.getProperty("db.user"),
                    BootEnvironmentPostProcessor.getProperty("db.passwd"));
            c.close();
        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
        return Status.success(name);
    }

    /**
     * @return
     */
    static Status checkCreateFile() {
        String name = "Create File";
        FileWriter fw = null;
        try {
            File test = new File(FileUtils.getTempDirectory(), "ServerStatus.test");
            fw = new FileWriter(test);
            IOUtils.write(CodecUtils.randomCode(1024), fw);
            if (!test.exists()) {
                return Status.error(name, "Cannot create file in temp-directory");
            } else {
                FileUtils.deleteQuietly(test);
            }

        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        } finally {
            IOUtils.closeQuietly(fw);
        }
        return Status.success(name);
    }

    /**
     * @return
     */
    static Status checkCacheService() {
        CommonsCache cache = Application.getCommonsCache();
        String name = "Cache";
        if (!Installer.isUseRedis()) name += "/EHCACHE";

        try {
            cache.putx("ServerStatus.test", 1, 60);
        } catch (Exception ex) {
            return Status.error(name, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
        return Status.success(name);
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
                log.debug("Checking " + this);
            } else {
                log.error("Checking " + this);
            }
        }

        private static Status success(String name) {
            return new Status(name, true, null);
        }

        private static Status error(String name, String error) {
            return new Status(name, false, error);
        }
    }

    // --

    private static final SystemInfo SI = new SystemInfo();

    /**
     * OS 内存用量
     *
     * @return [总计M, 已用%]
     */
    public static double[] getOsMemoryUsed() {
        GlobalMemory memory = SI.getHardware().getMemory();
        long memoryTotal = memory.getTotal();
        double memoryUsage = (memoryTotal - memory.getAvailable()) * 1.0 / memoryTotal;
        return new double[] {
                (int) (memoryTotal / MemoryInformationBean.MEGABYTES),
                ObjectUtils.round(memoryUsage * 100, 2)
        };
    }

    /**
     * JVM 内存用量
     *
     * @return [总计M, 已用%]
     */
    public static double[] getJvmMemoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        long memoryTotal = runtime.totalMemory();
        long memoryFree = runtime.freeMemory();
        double memoryUsage = (memoryTotal - memoryFree) * 1.0 / memoryTotal;
        return new double[] {
                (int) (memoryTotal / MemoryInformationBean.MEGABYTES),
                ObjectUtils.round(memoryUsage * 100, 2)
        };
    }


    /**
     * CPU 负载
     *
     * @return
     */
    public static double getSystemLoad() {
        double[] loadAverages = SI.getHardware().getProcessor().getSystemLoadAverage(2);
        return ObjectUtils.round(loadAverages[1], 2);
    }

    /**
     * 本机 IP
     *
     * @return
     */
    public static String getLocalIp() {
        List<NetworkIF> nets = SI.getHardware().getNetworkIFs();
        if (nets.isEmpty()) return "localhost";

        for (NetworkIF net : nets) {
            String[] ipsv4 = net.getIPv4addr();
            if (ipsv4.length > 0) return ipsv4[0];
        }
        return "127.0.0.1";
    }
}
