/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

/**
 * 审批过程中补充表单数据。但是因为审批中的记录不允许修改，所以在次数标记
 *
 * @author devezhao
 * @since 2020/9/28
 */
public class ApprovalContextHolder {

    private static final ThreadLocal<Boolean> ADDED_MODE = new ThreadLocal<>();

    public static void setAddedModeOnce() {
        ADDED_MODE.set(true);
    }

    public static boolean isAddedModeOnce(boolean clear) {
        Boolean is = ADDED_MODE.get();
        if (is != null && clear) {
            ADDED_MODE.remove();
        }
        return is != null && is;
    }

    public static void clear() {
        ADDED_MODE.remove();
    }
}
