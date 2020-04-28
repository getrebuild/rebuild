/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.charts;

/**
 * @author ZHAO
 * @since 2020/4/28
 */
public class ChartsHelper {

    /**
     * 0 值
     */
    public static final String VALUE_ZERO = "0";

    /**
     * 无值
     */
    public static final String VALUE_NONE = "无";

    /**
     * 是否 0 值
     *
     * @param value
     * @return
     */
    public static boolean isZero(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Double) {
            return (Double) value == 0d;
        } else if (value instanceof Long) {
            return (Long) value == 0L;
        } else if (value instanceof Integer) {
            return (Integer) value == 0;
        }
        return false;
    }
}
