/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.commons.CalendarUtils;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * monentjava
 *
 * @author devezhao
 * @since 09/23/2026
 */
@Slf4j
public class Moment2 {

    /**
     * @param unit W/M/Q/Y
     * @return
     */
    public static Date beginOfDate(String unit) {
        switch (unit) {
            case "W":
                return DateUtil.beginOfWeek(CalendarUtils.now());
            case "M":
                return DateUtil.beginOfMonth(CalendarUtils.now());
            case "Q":
                return DateUtil.beginOfQuarter(CalendarUtils.now());
            default:
                return DateUtil.beginOfYear(CalendarUtils.now());
        }
    }

    /**
     * @param date
     * @param unit   W/M/Q/Y
     * @param amount
     * @return
     */
    public static Date offsetDate(Date date, String unit, int amount) {
        switch (unit) {
            case "W":
                return DateUtil.offsetDay(date, amount * 7);
            case "M":
                return DateUtil.offsetMonth(date, amount);
            case "Q":
                return DateUtil.offsetMonth(date, amount * 3);
            default:
                return DateUtil.offsetMonth(date, amount * 12);
        }
    }

    /**
     * @param date
     * @param unit W/M/Q/Y
     * @return
     */
    public static Date endOfDate(Date date, String unit) {
        switch (unit) {
            case "W":
                return DateUtil.endOfWeek(date);
            case "M":
                return DateUtil.endOfMonth(date);
            case "Q":
                return DateUtil.endOfQuarter(date);
            default:
                return DateUtil.endOfYear(date);
        }
    }
}
