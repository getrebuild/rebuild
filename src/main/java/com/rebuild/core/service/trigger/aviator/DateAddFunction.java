/*!
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
import org.apache.commons.lang.math.NumberUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Usage: DATEADD($date, $number, [H|D|M|Y])
 * Return: Date
 *
 * @author devezhao
 * @since 2021/4/12
 */
public class DateAddFunction extends AbstractFunction {
    private static final long serialVersionUID = 8286269123891483078L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return call(env, arg1, arg2, AviatorNil.NIL);
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Object o = arg1.getValue(env);
        final Date $date = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
        if ($date == null) {
            return AviatorNil.NIL;
        }

        String $number = arg2.getValue(env) == null ? null : arg2.getValue(env).toString();
        if ($number == null) {
            return AviatorNil.NIL;
        }

        String $du = arg3.getValue(env) == null ? null : arg3.getValue(env).toString();

        // compatible: v2.8
        String numberLast = $number.substring($number.length() - 1);
        if (!NumberUtils.isNumber(numberLast)) {
            $du = numberLast;
            $number = $number.substring(0, $number.length() - 1);
        }

        int du4cal = Calendar.DATE;  // default
        if (AviatorDate.DU_MINUTE.equalsIgnoreCase($du)) du4cal = Calendar.MINUTE;
        else if (AviatorDate.DU_HOUR.equalsIgnoreCase($du)) du4cal = Calendar.HOUR_OF_DAY;
        else if (AviatorDate.DU_MONTH.equalsIgnoreCase($du)) du4cal = Calendar.MONTH;
        else if (AviatorDate.DU_YEAR.equalsIgnoreCase($du)) du4cal = Calendar.YEAR;

        Date newDate = dateAdd($date, ObjectUtils.toInt($number), du4cal);
        return new AviatorDate(newDate);
    }

    /**
     * @param date
     * @param interval
     * @param field
     * @return
     */
    protected Date dateAdd(Date date, int interval, int field) {
        return CalendarUtils.add(date, interval, field);
    }

    @Override
    public String getName() {
        return "DATEADD";
    }
}
