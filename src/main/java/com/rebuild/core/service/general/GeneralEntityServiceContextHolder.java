/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 *
 * @author devezhao
 * @since 2020/9/29
 */
public class GeneralEntityServiceContextHolder {

    private static final ThreadLocal<Boolean> SKIP_SERIES_VALUE = new ThreadLocal<>();

    private static final ThreadLocal<ID> ALLOW_FORCE_UPDATE = new ThreadLocal<>();

    /**
     * 新建记录时允许跳过自动编号字段
     */
    public static void setSkipSeriesValue() {
        SKIP_SERIES_VALUE.set(true);
    }

    /**
     * @param clear
     * @return
     * @see #setSkipSeriesValue()
     */
    public static boolean isSkipSeriesValue(boolean clear) {
        Boolean is = SKIP_SERIES_VALUE.get();
        if (is != null && clear) {
            SKIP_SERIES_VALUE.remove();
        }
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
        if (recordId != null) {
            ALLOW_FORCE_UPDATE.remove();
        }
        return recordId != null;
    }
}
