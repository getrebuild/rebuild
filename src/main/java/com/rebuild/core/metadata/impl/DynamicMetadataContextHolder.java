/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

/**
 * @author devezhao
 * @since 2020/9/29
 */
public class DynamicMetadataContextHolder {

    private static final ThreadLocal<Boolean> SKIP_REFENTITY_CHECK = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> SKIP_LANGUAGE_REFRESH = new ThreadLocal<>();

    /**
     * 跳过检测引用实体。
     * 主要用在实体导入时，被引用实体暂时不存在
     */
    public static void setSkipRefentityCheck() {
        SKIP_REFENTITY_CHECK.set(true);
    }

    /**
     * @param clear
     * @return
     * @see #setSkipRefentityCheck()
     */
    public static boolean isSkipRefentityCheck(boolean clear) {
        Boolean is = SKIP_REFENTITY_CHECK.get();
        if (is != null && clear) {
            SKIP_REFENTITY_CHECK.remove();
        }
        return is != null && is;
    }

    /**
     * 跳过语言刷新。主要用在批量导入实体时
     */
    public static void setSkipLanguageRefresh() {
        SKIP_LANGUAGE_REFRESH.set(true);
    }

    /**
     * @param clear
     * @return
     * @see #setSkipLanguageRefresh()
     */
    public static boolean isSkipLanguageRefresh(boolean clear) {
        Boolean is = SKIP_LANGUAGE_REFRESH.get();
        if (is != null && clear) {
            SKIP_LANGUAGE_REFRESH.remove();
        }
        return is != null && is;
    }
}
