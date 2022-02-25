/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

/**
 * Usage: DATEDIFF(date1, date2, [H|D|M|Y])
 * Return: Number
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class DateDiffFunction extends AbstractFunction {
    private static final long serialVersionUID = 5778729290544711131L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return call(env, arg1, arg2, new AviatorString(AviatorDate.DU_DAY));
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Object o1 = arg1.getValue(env);
        Date date1 = o1 instanceof Date ? (Date) o1 : CalendarUtils.parse(o1.toString());
        Object o2 = arg2.getValue(env);
        Date date2 = o2 instanceof Date ? (Date) o2 : CalendarUtils.parse(o2.toString());
        if (arg3.getValue(env) == null) {
            throw new ExpressionSyntaxErrorException("`dateUnit` cannot be null");
        }
        String dateUnit = arg3.getValue(env).toString();

        if (date1 == null) {
            log.warn("Parseing date1 error : `{}`. Use default", o1);
            date1 = CalendarUtils.now();
        }
        if (date2 == null) {
            log.warn("Parseing date2 error : `{}`. Use default", o2);
            date2 = CalendarUtils.now();
        }

        long timeLeft = date1.getTime() - date2.getTime();
        timeLeft = timeLeft / 1000 / 60 / 60;  // hours

        if (AviatorDate.DU_MONTH.equalsIgnoreCase(dateUnit)) timeLeft = timeLeft / 30;  // FIXME 月固定30天
        else if (AviatorDate.DU_YEAR.equalsIgnoreCase(dateUnit)) timeLeft = timeLeft / 365;  // FIXME 年固定365天
        else if (AviatorDate.DU_HOUR.equalsIgnoreCase(dateUnit)) ;
        else timeLeft = timeLeft / 24;

        return AviatorLong.valueOf(timeLeft);
    }

    @Override
    public String getName() {
        return "DATEDIFF";
    }
}
