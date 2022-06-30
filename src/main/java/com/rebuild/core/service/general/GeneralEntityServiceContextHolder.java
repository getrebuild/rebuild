/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * @author devezhao
 * @since 2020/9/29
 */
public class GeneralEntityServiceContextHolder {

    private static final ThreadLocal<Boolean> SKIP_SERIES_VALUE = new NamedThreadLocal<>("Skip series value");

    private static final ThreadLocal<ID> ALLOW_FORCE_UPDATE = new NamedThreadLocal<>("Allow force update");

    private static final ThreadLocal<Integer> REPEATED_CHECK_MODE = new NamedThreadLocal<>("Repeated check mode");

    private static final ThreadLocal<ID> FROM_TRIGGERS = new NamedThreadLocal<>("From triggers");

    /**
     * 新建记录时允许跳过自动编号字段
     */
    public static void setSkipSeriesValue() {
        SKIP_SERIES_VALUE.set(true);
    }

    /**
     * @param once
     * @return
     * @see #setSkipSeriesValue()
     */
    public static boolean isSkipSeriesValue(boolean once) {
        Boolean is = SKIP_SERIES_VALUE.get();
        if (is != null && once) SKIP_SERIES_VALUE.remove();
        return is != null && is;
    }

    /**
     * 允许强制修改（审批中的）记录
     *
     * @param recordId
     */
    public static void setAllowForceUpdate(ID recordId) {
        ALLOW_FORCE_UPDATE.set(recordId);
    }

    /**
     * @return
     * @see #setAllowForceUpdate(ID)
     */
    public static boolean isAllowForceUpdateOnce() {
        ID recordId = ALLOW_FORCE_UPDATE.get();
        if (recordId != null) ALLOW_FORCE_UPDATE.remove();
        return recordId != null;
    }

    /**
     * 从触发器执行允许跳过（某些）权限
     *
     * @param recordId
     */
    public static void setFromTrigger(ID recordId) {
        FROM_TRIGGERS.set(recordId);
    }

    /**
     * @return
     * @see #setFromTrigger(ID)
     */
    public static boolean isFromTrigger(boolean once) {
        ID recordId = FROM_TRIGGERS.get();
        if (recordId != null && once) FROM_TRIGGERS.remove();
        return recordId != null;
    }

    // 检查全部
    public static final int RCM_CHECK_MAIN = 1;
    // 检查主记录
    public static final int RCM_CHECK_DETAILS = 2;
    // 检查明细记录
    public static final int RCM_CHECK_ALL = 4;

    /**
     * 设定重复检查模式（仅在需要时设定）
     *
     * @param mode
     */
    public static void setRepeatedCheckMode(int mode) {
        REPEATED_CHECK_MODE.set(mode);
    }

    /**
     * @return
     * @see #setRepeatedCheckMode(int)
     */
    public static int getRepeatedCheckModeOnce() {
        Integer mode = REPEATED_CHECK_MODE.get();
        if (mode != null) REPEATED_CHECK_MODE.remove();
        return mode == null ? 0 : mode;
    }
}
