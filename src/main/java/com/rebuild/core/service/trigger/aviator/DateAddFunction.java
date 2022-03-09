/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Usage: DATEADD(date, interval[H|D|M|Y])
 * Return: Date
 *
 * @author devezhao
 * @since 2021/4/12
 */
public class DateAddFunction extends AbstractFunction {
    private static final long serialVersionUID = 8286269123891483078L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Object o = arg1.getValue(env);
        Date date = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
        if (date == null) {
            return AviatorNil.NIL;
        }

        String interval = arg2.getValue(env).toString();

        int unit4Interval = Calendar.DATE;  // default

        if (interval.endsWith(AviatorDate.DU_MINUTE)) {
            interval = interval.substring(0, interval.length() - 1);
            unit4Interval = Calendar.MINUTE;
        } else if (interval.endsWith(AviatorDate.DU_HOUR)) {
            interval = interval.substring(0, interval.length() - 1);
            unit4Interval = Calendar.HOUR_OF_DAY;
        } else if (interval.endsWith(AviatorDate.DU_DAY)) {
            interval = interval.substring(0, interval.length() - 1);
        } else if (interval.endsWith(AviatorDate.DU_MONTH)) {
            interval = interval.substring(0, interval.length() - 1);
            unit4Interval = Calendar.MONTH;
        } else if (interval.endsWith(AviatorDate.DU_YEAR)) {
            interval = interval.substring(0, interval.length() - 1);
            unit4Interval = Calendar.YEAR;
        }

        Date newDate = dateAdd(date, ObjectUtils.toInt(interval), unit4Interval);
        return new AviatorDate(newDate);
    }

    protected Date dateAdd(Date date, int interval, int field) {
        return CalendarUtils.add(date, interval, field);
    }

    @Override
    public String getName() {
        return "DATEADD";
    }
}
