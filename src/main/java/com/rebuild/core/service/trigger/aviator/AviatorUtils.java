/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionRuntimeException;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.googlecode.aviator.runtime.type.AviatorFunction;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * // https://www.yuque.com/boyan-avfmj/aviatorscript
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class AviatorUtils {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();

    static {
        // https://www.yuque.com/boyan-avfmj/aviatorscript/yr1oau
        AVIATOR.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, true);
        AVIATOR.setOption(Options.ENABLE_PROPERTY_SYNTAX_SUGAR, false);
        AVIATOR.setOption(Options.MAX_LOOP_COUNT, 32767);
        AVIATOR.setOption(Options.ALLOWED_CLASS_SET, Collections.emptySet());

        addCustomFunction(new DateDiffFunction());
        addCustomFunction(new DateAddFunction());
        addCustomFunction(new DateSubFunction());
        addCustomFunction(new CurrentUserFunction());
        addCustomFunction(new CurrentBizunitFunction());
        addCustomFunction(new CurrentDateFunction());
        addCustomFunction(new LocationDistanceFunction());
        addCustomFunction(new RequestFunctuin());
    }

    /**
     * 表达式计算
     *
     * @param expression
     * @return
     */
    public static Object evalQuietly(String expression) {
        return eval(expression, null, true);
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
            return AVIATOR.execute(expression, env);
        } catch (ArithmeticException | ExpressionRuntimeException ex) {
            if (quietly) log.error("Bad expression : `{}` < {}", expression, env, ex);
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
     * 添加自定义函数（函数名区分大小写）
     *
     * @param function
     */
    public static void addCustomFunction(final AviatorFunction function) {
        log.info("Add custom function : {}", function);
        AVIATOR.addFunction(function);
    }

    /**
     * @return
     */
    public static AviatorEvaluatorInstance getInstance() {
        return AVIATOR;
    }

}
