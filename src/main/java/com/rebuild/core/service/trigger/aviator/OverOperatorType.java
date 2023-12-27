/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import com.googlecode.aviator.lexer.token.OperatorType;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorLong;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.Date;
import java.util.Map;

/**
 * 操作符重载
 *
 * @author RB
 * @since 2023/12/6
 */
public class OverOperatorType {

    private OverOperatorType() {}

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
            Object $argv1 = arg1.getValue(env);
            Object $argv2 = arg2.getValue(env);

            if ($argv1 instanceof Date && $argv2 instanceof Number) {
                return opDate((Date) $argv1, ((Number) $argv2).intValue());
            } else if ($argv2 instanceof Date && $argv1 instanceof Number) {
                return opDate((Date) $argv2, ((Number) $argv1).intValue());
            } else {
                return arg1.add(arg2, env);  // Use default
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
            Object $argv1 = arg1.getValue(env);
            Object $argv2 = arg2.getValue(env);

            if ($argv1 instanceof Date && $argv2 instanceof Number) {
                return opDate((Date) $argv1, -((Number) $argv2).intValue());
            } else if ($argv2 instanceof Date && $argv1 instanceof Number) {
                return opDate((Date) $argv2, -((Number) $argv1).intValue());
            } else if ($argv1 instanceof Date && $argv2 instanceof Date) {
                int diff = CalendarUtils.getDayLeft((Date) $argv1, (Date) $argv2);
                return AviatorLong.valueOf(diff);
            } else {
                return arg1.add(arg2, env);  // Use default
            }
        }
    }

    static AviatorDate opDate(Date date, int num) {
        Date d = CalendarUtils.addDay(date, num);
        return new AviatorDate(d);
    }

    // -- 比较

    /**
     * 日期比较
     */
    static abstract class DateCompare extends AbstractFunction {
        private static final long serialVersionUID = -4160230503581309424L;
        @Override
        public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
            Object $argv1 = arg1.getValue(env);
            Object $argv2 = arg2.getValue(env);

            if ($argv1 instanceof Date && $argv2 instanceof Date) {
                long v1 = ((Date) $argv1).getTime();
                long v2 = ((Date) $argv2).getTime();
                boolean b = compare(v1, v2);
                return AviatorBoolean.valueOf(b);
            } else {
                return arg1.add(arg2, env);  // Use default
            }
        }

        abstract protected boolean compare(long v1, long v2);
    }

    // LE `<=`
    static class DateCompareLE extends DateCompare {
        private static final long serialVersionUID = 1321662048697121893L;
        @Override
        public String getName() {
            return OperatorType.LE.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 <= v2;
        }
    }
    // LT `<`
    static class DateCompareLT extends DateCompare {
        private static final long serialVersionUID = 8197857653882782806L;
        @Override
        public String getName() {
            return OperatorType.LT.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 < v2;
        }
    }
    // GE `>=`
    static class DateCompareGE extends DateCompare {
        private static final long serialVersionUID = -7966630104916265372L;
        @Override
        public String getName() {
            return OperatorType.GE.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 >= v2;
        }
    }
    // GT `>`
    static class DateCompareGT extends DateCompare {
        private static final long serialVersionUID = 5214573679573440753L;
        @Override
        public String getName() {
            return OperatorType.GT.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 > v2;
        }
    }
    // EQ `==`
    static class DateCompareEQ extends DateCompare {
        private static final long serialVersionUID = -6142749075506832977L;
        @Override
        public String getName() {
            return OperatorType.EQ.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 == v2;
        }
    }
    // NEQ `!=`
    static class DateCompareNEQ extends DateCompare {
        private static final long serialVersionUID = -838391653977975466L;
        @Override
        public String getName() {
            return OperatorType.NEQ.getToken();
        }
        @Override
        protected boolean compare(long v1, long v2) {
            return v1 != v2;
        }
    }
}
