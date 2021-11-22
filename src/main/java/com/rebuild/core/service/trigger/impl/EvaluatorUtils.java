/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionRuntimeException;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorString;
import com.googlecode.aviator.runtime.type.AviatorType;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * // https://www.yuque.com/boyan-avfmj/aviatorscript
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class EvaluatorUtils {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();
    static {
        // https://www.yuque.com/boyan-avfmj/aviatorscript/yr1oau
        // 强制使用 BigDecimal/BigInteger 运算
        AVIATOR.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, true);
        // 关闭语法糖
        AVIATOR.setOption(Options.ENABLE_PROPERTY_SYNTAX_SUGAR, false);

        // 函数（函数名区分大小写）
        AVIATOR.addFunction(new DateDiffFunction());
        AVIATOR.addFunction(new DateAddFunction());
        AVIATOR.addFunction(new DateSubFunction());
    }

    /**
     * 表达式计算
     *
     * @param expression
     * @return
     */
    public static Object eval(String expression) {
        return eval(expression, null, true);
    }

    /**
     * 表达式计算
     *
     * @param expression
     * @param env
     * @param quietly true 不抛出异常
     * @return
     */
    public static Object eval(String expression, Map<String, Object> env, boolean quietly) {
        try {
            return AVIATOR.execute(expression, env);
        } catch (ArithmeticException | ExpressionRuntimeException ex) {
            if (quietly) log.error("Bad expression : `{}`", expression, ex);
            else throw ex;
        }
        return null;
    }

    /**
     * 语法验证
     *
     * @param expression
     * @return
     */
    public static boolean validate(String expression) {
        try {
            getInstance().validate(expression);
            return true;
        } catch (ExpressionSyntaxErrorException ex) {
            log.warn("Bad expression : `{}`", expression);
            return false;
        }
    }

    /**
     * @return
     */
    public static AviatorEvaluatorInstance getInstance() {
        return AVIATOR;
    }

    // --

    private static final String DU_HOUR = "H";
    private static final String DU_DAY = "D";
    private static final String DU_MONTH = "M";
    private static final String DU_YEAR = "Y";

    static class AviatorDate extends AviatorObject {
        private static final long serialVersionUID = 2930549924386648595L;

        private Date dateValue;

        protected AviatorDate(Date value) {
            super();
            this.dateValue = value;
        }

        @Override
        public int innerCompare(AviatorObject other, Map<String, Object> env) {
            return 0;
        }

        @Override
        public AviatorType getAviatorType() {
            return AviatorType.JavaType;
        }

        @Override
        public Object getValue(Map<String, Object> env) {
            return this.dateValue;
        }
    }

    // 日期差值 `number = DATEDIFF(date1, date2, [H|D|M|Y])`
    static class DateDiffFunction extends AbstractFunction {
        private static final long serialVersionUID = 5778729290544711131L;

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            return call(env, arg1, arg2, new AviatorString(DU_DAY));
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
            String dateUnit = arg3.getValue(env) .toString();

            if (date1 == null) {
                log.warn("Parseing date1 error : `{}`. Use default", o1);
                date1 = CalendarUtils.now();
            }
            if (date2 == null) {
                log.warn("Parseing date2 error : `{}`. Use default", o2);
                date2 = CalendarUtils.now();
            }

            long timeLeft = date1.getTime() - date2.getTime();
            timeLeft = timeLeft / 1000 / 60 / 60;  // Hours

            if (DU_MONTH.equalsIgnoreCase(dateUnit)) timeLeft = timeLeft / 30;  // FIXME 月固定30天
            else if (DU_YEAR.equalsIgnoreCase(dateUnit)) timeLeft = timeLeft / 365;  // FIXME 年固定365天
            else if (DU_HOUR.equalsIgnoreCase(dateUnit));
            else timeLeft = timeLeft / 24;

            return AviatorLong.valueOf(timeLeft);
        }

        @Override
        public String getName() {
            return "DATEDIFF";
        }
    }

    // 日期加 `date = DATEADD(date, interval[H|D|M|Y])`
    static class DateAddFunction extends AbstractFunction {
        private static final long serialVersionUID = 8286269123891483078L;

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object o = arg1.getValue(env);
            Date date = o instanceof Date ? (Date) o : CalendarUtils.parse(o.toString());
            String interval = arg2.getValue(env).toString();

            int intervalUnit = Calendar.DATE;

            if (interval.endsWith(DU_DAY)) {
                interval = interval.substring(0, interval.length() - 1);
            } else if (interval.endsWith(DU_MONTH)) {
                interval = interval.substring(0, interval.length() - 1);
                intervalUnit = Calendar.MONTH;
            } else if (interval.endsWith(DU_YEAR)) {
                interval = interval.substring(0, interval.length() - 1);
                intervalUnit = Calendar.YEAR;
            } else if (interval.endsWith(DU_HOUR)) {
                interval = interval.substring(0, interval.length() - 1);
                intervalUnit = Calendar.HOUR_OF_DAY;
            }

            Date newDate = dateCalc(date, ObjectUtils.toInt(interval), intervalUnit);
            return new AviatorDate(newDate);
        }

        protected Date dateCalc(Date date, int interval, int field) {
            return CalendarUtils.add(date, interval, field);
        }

        @Override
        public String getName() {
            return "DATEADD";
        }
    }

    // 日期减 `date = DATESUB(date, interval[H|D|M|Y])`
    static class DateSubFunction extends DateAddFunction {
        private static final long serialVersionUID = -1002040162587992573L;

        @Override
        protected Date dateCalc(Date date, int interval, int field) {
            return super.dateCalc(date, -interval, field);
        }

        @Override
        public String getName() {
            return "DATESUB";
        }
    }
}
