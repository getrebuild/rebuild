/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.runtime.MemoryInformationBean;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.util.List;

/**
 * @author devezhao
 * @since 2021/9/22
 */
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
        double memoryUsage = (memoryTotal - memory.getAvailable()) * 1.0 / memoryTotal;
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

        for (NetworkIF net : nets) {
            String[] ipsv4 = net.getIPv4addr();
            if (ipsv4 != null && ipsv4.length > 0) return ipsv4[0];
        }
        return "127.0.0.1";
    }
}
