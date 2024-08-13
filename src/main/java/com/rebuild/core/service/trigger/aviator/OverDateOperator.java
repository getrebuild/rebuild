/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.lexer.token.OperatorType;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.Date;
import java.util.Map;

/**
 * 日期相关操作符重载
 *
 * @author RB
 * @since 2023/12/6
 */
public class OverDateOperator {

    private OverDateOperator() {}

    // -- 计算

    /**
     * 日期加
     */
    static class DateAdd extends AbstractFunction {
        private static final long serialVersionUID = -7871678038170332371L;
        @Override
        public String getName() {
            return OperatorType.ADD.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Number) {
                return opDate((Date) $argv1, ((Number) $argv2).intValue());
            } else if ($argv2 instanceof Date && $argv1 instanceof Number) {
                return opDate((Date) $argv2, ((Number) $argv1).intValue());
            } else {
                return arg1.add(arg2, env);
            }
        }
    }

    /**
     * 日期减
     */
    static class DateSub extends AbstractFunction {
        private static final long serialVersionUID = 8208361199770129766L;
        @Override
        public String getName() {
            return OperatorType.SUB.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Number) {
                return opDate((Date) $argv1, -((Number) $argv2).intValue());
            } else if ($argv2 instanceof Date && $argv1 instanceof Number) {
                return opDate((Date) $argv2, -((Number) $argv1).intValue());
            } else if ($argv1 instanceof Date && $argv2 instanceof Date) {
                int diff = CalendarUtils.getDayLeft((Date) $argv1, (Date) $argv2);
                return AviatorLong.valueOf(diff);
            } else {
                return arg1.sub(arg2, env);
            }
        }
    }

    static AviatorDate opDate(Date date, int num) {
        Date d = CalendarUtils.addDay(date, num);
        return new AviatorDate(d);
    }

    // -- 比较

    /**
     * 日期比较: LE `<=`
     */
    static class DateCompareLE extends AbstractFunction {
        private static final long serialVersionUID = 1321662048697121893L;
        @Override
        public String getName() {
            return OperatorType.LE.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 <= v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s <= 0);
            }
        }
    }

    /**
     * 日期比较: LT `<`
     */
    static class DateCompareLT extends AbstractFunction {
        private static final long serialVersionUID = 8197857653882782806L;
        @Override
        public String getName() {
            return OperatorType.LT.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 < v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s < 0);
            }
        }
    }

    /**
     * 日期比较: GE `>=`
     */
    static class DateCompareGE extends AbstractFunction {
        private static final long serialVersionUID = -7966630104916265372L;
        @Override
        public String getName() {
            return OperatorType.GE.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 >= v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s >= 0);
            }
        }
    }

    /**
     * 日期比较: GT `>`
     */
    static class DateCompareGT extends AbstractFunction {
        private static final long serialVersionUID = 5214573679573440753L;
        @Override
        public String getName() {
            return OperatorType.GT.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 > v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s > 0);
            }
        }
    }

    /**
     * 日期比较: EQ `==`
     */
    static class DateCompareEQ extends AbstractFunction {
        private static final long serialVersionUID = -6142749075506832977L;
        @Override
        public String getName() {
            return OperatorType.EQ.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 == v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s == 0);
            }
        }
    }

    /**
     * 日期比较: NEQ `!=`
     */
    static class DateCompareNEQ extends AbstractFunction {
        private static final long serialVersionUID = -838391653977975466L;
        @Override
        public String getName() {
            return OperatorType.NEQ.getToken();
        }

        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = convertIfDate(arg1.getValue(env));
            Object $argv2 = convertIfDate(arg2.getValue(env));

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                return FunctionUtils.wrapReturn(v1 != v2);
            } else {
                int s = arg1.compare(arg2, env);
                return FunctionUtils.wrapReturn(s != 0);
            }
        }
    }

    // 转换为日期
    static Object convertIfDate(Object d) {
        if (d instanceof Date) return d;
        if (d instanceof String) {
            Date date = CalendarUtils.parse((String) d);
            return date == null ? d : date;
        }
        return d;
    }
}
