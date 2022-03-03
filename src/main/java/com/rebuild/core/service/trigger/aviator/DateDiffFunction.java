/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.rebuild.core.Application;
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

    private boolean isUseMysql;

    protected DateDiffFunction() {
        this(Boolean.FALSE);
    }

    protected DateDiffFunction(boolean isUseMysql) {
        super();
        this.isUseMysql = isUseMysql;
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        return call(env, arg1, arg2, new AviatorString(AviatorDate.DU_DAY));
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
        Object o1 = arg1.getValue(env);
        Date date1 = o1 instanceof Date ? (Date) o1 : CalendarUtils.parse(o1.toString());
        if (date1 == null) {
            return AviatorNil.NIL;
        }

        Object o2 = arg2.getValue(env);
        Date date2 = o2 instanceof Date ? (Date) o2 : CalendarUtils.parse(o2.toString());
        if (date2 == null) {
            return AviatorNil.NIL;
        }

        if (arg3.getValue(env) == null) {
            throw new ExpressionSyntaxErrorException("`dateUnit` cannot be null");
        }

        String dateUnit = arg3.getValue(env).toString();

        if (isUseMysql) {
            String mysqlUnit = "DAY";
            if (AviatorDate.DU_YEAR.equalsIgnoreCase(dateUnit)) mysqlUnit = "YEAR";
            else if (AviatorDate.DU_MONTH.equalsIgnoreCase(dateUnit)) mysqlUnit = "MONTH";
            else if (AviatorDate.DU_HOUR.equalsIgnoreCase(dateUnit)) mysqlUnit = "HOUR";
            else if (AviatorDate.DU_MINUTE.equalsIgnoreCase(dateUnit)) mysqlUnit = "MINUTE";

            // 利用 MySQL 计算，可预期
            String mysql = String.format("select TIMESTAMPDIFF(%s, '%s', '%s')",
                    mysqlUnit,
                    CalendarUtils.getUTCDateTimeFormat().format(date1),
                    CalendarUtils.getUTCDateTimeFormat().format(date2));
            Object[] res = Application.getPersistManagerFactory().createNativeQuery(mysql).unique();

            return AviatorLong.valueOf(ObjectUtils.toLong(res[0]));

        } else {

            long res = 0;

            if (AviatorDate.DU_YEAR.equalsIgnoreCase(dateUnit)) res = DateUtil.betweenYear(date1, date2, true);
            else if (AviatorDate.DU_MONTH.equalsIgnoreCase(dateUnit)) res = DateUtil.betweenMonth(date1, date2, true);
            else if (AviatorDate.DU_DAY.equalsIgnoreCase(dateUnit)) res = DateUtil.betweenDay(date1, date2, true);
            else if (AviatorDate.DU_HOUR.equalsIgnoreCase(dateUnit)) res = DateUtil.between(date1, date2, DateUnit.HOUR, false);
            else if (AviatorDate.DU_MINUTE.equalsIgnoreCase(dateUnit)) res = DateUtil.between(date1, date2, DateUnit.MINUTE, false);

            return AviatorLong.valueOf(res);
        }
    }

    @Override
    public String getName() {
        return "DATEDIFF";
    }
}
