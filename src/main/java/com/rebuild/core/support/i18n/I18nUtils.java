/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.commons.CalendarUtils;

import java.util.Date;

/**
 * @author ZHAO
 * @since 2020/09/17
 */
public class I18nUtils {

    /**
     * 客户端所需的日期时间格式（带时区偏移）
     *
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        if (date == null) return null;
        return CalendarUtils.getUTCWithZoneDateTimeFormat().format(date);
    }

    /**
     * @param key
     * @param placeholders
     * @return
     * @see Language#L(String, Object...)
     */
    public static String L(String key, Object... placeholders) {
        return Language.L(key, placeholders);
    }
}
