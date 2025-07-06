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
import oshi.software.os.OSFileStore;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
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
            net.updateAttributes();
            if (net.isKnownVmMacAddr()) continue;
            if (net.getIfOperStatus() != NetworkIF.IfOperStatus.UP) continue;

            String name = net.getName().toLowerCase();
            if (name.contains("docker") || name.contains("vbox") || name.contains("vmnet")
                    || name.contains("loopback") || name.contains("veth")) {
                continue;
            }

            for (String ip : net.getIPv4addr()) {
                if (StringUtils.isBlank(ip) || ip.equals("127.0.0.1") || ip.equals("0.0.0.0")) continue;
                if (bestipv4 == null) bestipv4 = ip;
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
                "https://getrebuild.com/",
        };

        for (String u : FROMURLS) {
            try {
                URLConnection conn = new URL(u).openConnection();
                final long L = conn.getDate();

                if (L > 0) {
                    Calendar c = CalendarUtils.getInstance();
                    c.setTimeInMillis(L);
                    return c.getTime();
                }
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
     * @param specRoots
     * @return [总大小, 占用%, 磁盘]
     */
    public static List<Object[]> getDisksUsed(String[] specRoots) {
        List<Object[]> disks = new ArrayList<>();
        try {
            if (specRoots != null) {
                File[] listRoots = File.listRoots();
                if (specRoots.length > 0) {
                    listRoots = new File[specRoots.length];
                    for (int i = 0; i < specRoots.length; i++) {
                        CommonsUtils.checkSafeFilePath(specRoots[i]);
                        listRoots[i] = new File(specRoots[i]);
                    }
                }

                for (File root : listRoots) {
                    if (!root.exists()) continue;
                    String name = StringUtils.defaultIfBlank(root.getName(), root.getPath());
                    double total = (double) root.getTotalSpace() / FileUtils.ONE_GB;
                    double used = total - ((double) root.getFreeSpace() / FileUtils.ONE_GB);
                    double usedPercentage = used * 100d / total;
                    disks.add(new Object[] { ObjectUtils.round(total, 1), ObjectUtils.round(usedPercentage, 1), name });
                }
            } else {
                for (OSFileStore store : getSI().getOperatingSystem().getFileSystem().getFileStores()) {
                    String name = store.getName();
                    double total = store.getTotalSpace() * 1d / FileUtils.ONE_GB;
                    double used = total - store.getUsableSpace() * 1d / FileUtils.ONE_GB;
                    double usedPercentage = used * 100d / total;
                    disks.add(new Object[] { ObjectUtils.round(total, 1), ObjectUtils.round(usedPercentage, 1), name });
                }
            }
        } catch (Exception ex) {
            log.warn("Cannot stats disks", ex);
        }
        return disks;
    }
}
