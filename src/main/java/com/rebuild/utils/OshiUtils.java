/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.runtime.MemoryInformationBean;
import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

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
                ObjectUtils.round(memoryUsage * 100, 2)
        };
    }

    /**
     * JVM 内存
     *
     * @return
     */
    public static double[] getJvmMemoryUsed() {
//        double maxMemory = Runtime.getRuntime().maxMemory();
        double memoryTotal = Runtime.getRuntime().totalMemory();
        double memoryFree = Runtime.getRuntime().freeMemory();
        double memoryUsage = (memoryTotal - memoryFree) / memoryTotal;
        return new double[]{
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
        double[] loadAverages = getSI().getHardware().getProcessor().getSystemLoadAverage(2);
        return ObjectUtils.round(loadAverages[1], 2);
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

        return StringUtils.defaultString(bestipv4, "127.0.0.1");
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
}
