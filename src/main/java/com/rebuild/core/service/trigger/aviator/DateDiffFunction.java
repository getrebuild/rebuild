/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.hutool.core.date.DateBetween;
import cn.hutool.core.date.DateUnit;
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
 * Usage: DATEDIFF($date1, $date2, [H|D|M|Y])
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
        Object o = arg1.getValue(env);
        Date $date1 = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
        if ($date1 == null) {
            return AviatorNil.NIL;
        }

        o = arg2.getValue(env);
        Date $date2 = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
        if ($date2 == null) {
            return AviatorNil.NIL;
        }

        final String $du = arg3.getValue(env) == null ? null : arg3.getValue(env).toString();

        if (isUseMysql) {
            String mysqlUnit = "DAY";
            if (AviatorDate.DU_YEAR.equalsIgnoreCase($du)) mysqlUnit = "YEAR";
            else if (AviatorDate.DU_MONTH.equalsIgnoreCase($du)) mysqlUnit = "MONTH";
            else if (AviatorDate.DU_HOUR.equalsIgnoreCase($du)) mysqlUnit = "HOUR";
            else if (AviatorDate.DU_MINUTE.equalsIgnoreCase($du)) mysqlUnit = "MINUTE";
            else if (AviatorDate.DU_SECOND.equalsIgnoreCase($du)) mysqlUnit = "SECOND";

            // 利用 MySQL 计算，可预期
            String mysql = String.format("select TIMESTAMPDIFF(%s, '%s', '%s')",
                    mysqlUnit,
                    CalendarUtils.getUTCDateTimeFormat().format($date1),
                    CalendarUtils.getUTCDateTimeFormat().format($date2));
            Object[] res = Application.getPersistManagerFactory().createNativeQuery(mysql).unique();

            return AviatorLong.valueOf(ObjectUtils.toLong(res[0]));

        } else {

            long res = 0;
            DateBetween between = DateBetween.create($date1, $date2, Boolean.FALSE);

            if (AviatorDate.DU_YEAR.equalsIgnoreCase($du)) res = between.betweenYear(Boolean.TRUE);
            else if (AviatorDate.DU_MONTH.equalsIgnoreCase($du)) res = between.betweenMonth(Boolean.TRUE);
            else if (AviatorDate.DU_DAY.equalsIgnoreCase($du)) res = between.between(DateUnit.DAY);
            else if (AviatorDate.DU_HOUR.equalsIgnoreCase($du)) res = between.between(DateUnit.HOUR);
            else if (AviatorDate.DU_MINUTE.equalsIgnoreCase($du)) res = between.between(DateUnit.MINUTE);
            else if (AviatorDate.DU_SECOND.equalsIgnoreCase($du)) res = between.between(DateUnit.SECOND);

            return AviatorLong.valueOf(res);
        }
    }

    @Override
    public String getName() {
        return "DATEDIFF";
    }
}
