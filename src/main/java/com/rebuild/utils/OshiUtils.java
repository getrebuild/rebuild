/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.runtime.MemoryInformationBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author devezhao
 * @since 2021/9/22
 */
@Slf4j
public class OshiUtils {

    private static SystemInfo SI;

    /**
     * @return
     */
    synchronized public static SystemInfo getSI() {
        if (SI == null) SI = new SystemInfo();
        return SI;
    }

    /**
     * OS 内存
     *
     * @return
     */
    public static double[] getOsMemoryUsed() {
        GlobalMemory memory = getSI().getHardware().getMemory();
        long memoryTotal = memory.getTotal();
        long memoryFree = memory.getAvailable();
        double memoryUsage = (memoryTotal - memoryFree) * 1.0 / memoryTotal;
        return new double[]{
                (int) (memoryTotal / MemoryInformationBean.MEGABYTES),
                ObjectUtils.round(memoryUsage * 100, 1)
        };
    }

    /**
     * JVM 内存
     *
     * @return
     */
    public static double[] getJvmMemoryUsed() {
        double memoryTotal = Runtime.getRuntime().totalMemory();
        double memoryFree = Runtime.getRuntime().freeMemory();
        double memoryUsage = (memoryTotal - memoryFree) / memoryTotal;
        return new double[]{
                (int) (memoryTotal / MemoryInformationBean.MEGABYTES),
                ObjectUtils.round(memoryUsage * 100, 1)
        };
    }

    /**
     * CPU 负载
     *
     * @return
     */
    public static double getSystemLoad() {
        double[] loadAverages = getSI().getHardware().getProcessor().getSystemLoadAverage(2);
        return ObjectUtils.round(loadAverages[1], 1);
    }

    /**
     * 本机 IP
     *
     * @return
     */
    public static String getLocalIp() {
        List<NetworkIF> nets = getSI().getHardware().getNetworkIFs();
        if (nets == null || nets.isEmpty()) return "localhost";

        String bestipv4 = null;
        for (NetworkIF net : nets) {
            for (String ip : net.getIPv4addr()) {
                if (bestipv4 == null) bestipv4 = ip;
                break;
            }

            if (net.isKnownVmMacAddr()) continue;

            String[] ipsv4 = net.getIPv4addr();
            if (ipsv4.length > 0) {
                bestipv4 = ipsv4[0];
                break;
            }
        }

        return StringUtils.defaultIfBlank(bestipv4, "127.0.0.1");
    }

    /**
     * 获取网络时间
     *
     * @return
     */
    public static Date getNetworkDate() {
        final String[] FROMURLS = new String[] {
                "https://www.baidu.com/",
                "https://www.microsoft.com/",
        };

        for (String u : FROMURLS) {
            try {
                URLConnection conn = new URL(u).openConnection();
                long l = conn.getDate();
                return new Date(l);
            } catch (Exception ex) {
                log.warn("Cannot fetch date from : {}", u, ex);
            }
        }

        return CalendarUtils.now();
    }

    /**
     * 是否 Docker 环境
     * https://github.com/alexvyber/isdocker/blob/main/index.cjs.js
     *
     * @return
     */
    public static boolean isDockerEnv() {
        try {
            // #1
            if (new File("/.dockerenv").exists()) return true;
        } catch (Exception ignored) {
            // #2
            try (Stream<String> stream = Files.lines(Paths.get("/proc/self/cgroup"))) {
                return stream.anyMatch(l -> l.contains("docker"));
            } catch (Exception ignored2) {
            }
        }
        return false;
    }

    /**
     * 磁盘用量获取
     *
     * @return [统计, 占用%, 磁盘]
     */
    public static List<Object[]> getDisksUsed() {
        List<Object[]> disks = new ArrayList<>();
        try {
            for (File root : File.listRoots()) {
                String name = org.apache.commons.lang.StringUtils.defaultIfBlank(root.getName(), root.getAbsolutePath());
                double total = root.getTotalSpace() * 1d / FileUtils.ONE_GB;
                double used = total - (root.getFreeSpace() * 1d / FileUtils.ONE_GB);
                double usedPercentage = used * 100d / total;
                disks.add(new Object[] { total, usedPercentage, name });
            }
        } catch (Exception ex) {
            log.warn("Cannot stats disks", ex);
        }
        return disks;
    }
}
