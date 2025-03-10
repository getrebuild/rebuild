/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.googlecode.aviator.exception.StandardError;
import com.googlecode.aviator.lexer.token.OperatorType;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.function.system.AssertFunction;
import com.googlecode.aviator.runtime.type.AviatorFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.Sequence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * https://www.yuque.com/boyan-avfmj/aviatorscript
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class AviatorUtils {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();

    static {
        // https://www.yuque.com/boyan-avfmj/aviatorscript/yr1oau
        AVIATOR.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, Boolean.TRUE);
        AVIATOR.setOption(Options.ALWAYS_PARSE_INTEGRAL_NUMBER_INTO_DECIMAL, Boolean.TRUE);
        AVIATOR.setOption(Options.ENABLE_PROPERTY_SYNTAX_SUGAR, Boolean.FALSE);
        AVIATOR.setOption(Options.ALLOWED_CLASS_SET, Collections.emptySet());
        AVIATOR.setOption(Options.TRACE_EVAL, Boolean.FALSE);

        try {
            // https://commons.apache.org/proper/commons-lang/javadocs/api-release/index.html
            AVIATOR.addStaticFunctions("StringUtils", StringUtils.class);
        } catch (Exception ignored) {
        }

        // 重载操作符
        AVIATOR.addOpFunction(OperatorType.ADD, new OverDateOperator.DateAdd());
        AVIATOR.addOpFunction(OperatorType.SUB, new OverDateOperator.DateSub());
        AVIATOR.addOpFunction(OperatorType.LE, new OverDateOperator.DateCompareLE());
        AVIATOR.addOpFunction(OperatorType.LT, new OverDateOperator.DateCompareLT());
        AVIATOR.addOpFunction(OperatorType.GE, new OverDateOperator.DateCompareGE());
        AVIATOR.addOpFunction(OperatorType.GT, new OverDateOperator.DateCompareGT());
        AVIATOR.addOpFunction(OperatorType.EQ, new OverDateOperator.DateCompareEQ());
        AVIATOR.addOpFunction(OperatorType.NEQ, new OverDateOperator.DateCompareNEQ());

        // 自定义函数
        addCustomFunction(new DateDiffFunction());
        addCustomFunction(new DateAddFunction());
        addCustomFunction(new DateSubFunction());
        addCustomFunction(new CurrentUserFunction());
        addCustomFunction(new CurrentBizunitFunction());
        addCustomFunction(new CurrentDateFunction());
        addCustomFunction(new ChineseYuanFunction());
        addCustomFunction(new TextFunction());
        addCustomFunction(new IsNullFunction());
        addCustomFunction(new ChineseDateFunction());
    }

    /**
     * @param expression
     * @return
     * @see #eval(String, Map, boolean)
     */
    public static Object eval(String expression) {
        return eval(expression, null, false);
    }

    /**
     * @param expression
     * @return
     * @see #eval(String, Map, boolean)
     */
    public static Object eval(String expression, Map<String, Object> env) {
        return eval(expression, env, false);
    }

    /**
     * 表达式计算
     *
     * @param expression
     * @param env
     * @param quietly true 表示不抛出异常
     * @return
     */
    public static Object eval(String expression, Map<String, Object> env, boolean quietly) {
        try {
            return AVIATOR.execute(expression, env == null ? Collections.emptyMap() : env);
        } catch (Exception ex) {
            if (ex instanceof AssertFunction.AssertFailed) {
                throw new AssertFailedException((AssertFunction.AssertFailed) ex);
            }

            if (!StandardError.class.getName().equals(ex.getClass().getName())) {
                log.error("Bad aviator expression : \n>> {}\n>> {}\n>> {}", expression, env, ex.getLocalizedMessage());
            }
            if (!quietly) throw ex;
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
            log.warn("Bad aviator expression : `{}`", expression);
            return false;
        }
    }

    /**
     * 添加自定义函数（函数名区分大小写）
     *
     * @param function
     */
    public static void addCustomFunction(final AviatorFunction function) {
        log.info("Add custom function : {}", function.getName());
        AVIATOR.addFunction(function);
    }

    /**
     * @return
     */
    public static AviatorEvaluatorInstance getInstance() {
        return AVIATOR;
    }

    /**
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Object> toIterator(Object value) {
        if (value instanceof Collection) return ((Collection<Object>) value).iterator();
        if (value instanceof Sequence) return ((Sequence<Object>) value).iterator();

        throw new UnsupportedOperationException("Unsupport type : " + value);
    }

    /**
     * @param ret
     * @return
     * @see FunctionUtils#wrapReturn(Object)
     */
    public static AviatorObject wrapReturn(final Object ret) {
        if (ret == null) return AviatorNil.NIL;
        if (ret instanceof Date) return new AviatorDate((Date) ret);
        if (ret instanceof ID) return new AviatorId((ID) ret);
        return FunctionUtils.wrapReturn(ret);
    }
}
