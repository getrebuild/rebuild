/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
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
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.core.support.state.StateHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * https://www.yuque.com/boyan-avfmj/aviatorscript
 *
 * @author devezhao
 * @since 2021/4/12
 */
@Slf4j
public class AviatorUtils {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();

    private static final Set<String> CUSTOM_FUNCTIONS = new TreeSet<>();

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
        addCustomFunction(new DateFunction());
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
        CUSTOM_FUNCTIONS.add(function.getName());
    }

    /**
     * AVIATOR 实例
     *
     * @return
     */
    public static AviatorEvaluatorInstance getInstance() {
        return AVIATOR;
    }

    /**
     * 自定义函数
     *
     * @return
     */
    public static Set<String> getCustomFunctionNames() {
        return CUSTOM_FUNCTIONS;
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
        if (ret instanceof LocalTime) return new AviatorTime((LocalTime) ret);
        if (ret instanceof ID) return new AviatorId((ID) ret);
        return FunctionUtils.wrapReturn(ret);
    }

    /**
     * @param o
     * @return
     */
    public static ID toIdValue(Object o) {
        if (o instanceof ID) return (ID) o;

        String o2str = o.toString().trim();
        if (o2str.isEmpty()) return null;
        if (ID.isId(o2str)) return ID.valueOf(o2str);

        log.warn("Bad id string : {}", o);
        return null;
    }

    /**
     * @param o
     * @return
     */
    public static String toStringValue(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    /**
     * 提取公式的字段变量
     *
     * @param formula
     * @param checkIfNeed 不传则不验证字段
     * @return
     */
    public static Set<String> matchsFieldVars(String formula, Entity checkIfNeed) {
        Set<String> fieldVars = new HashSet<>();
        Set<String> matchsVars = ContentWithFieldVars.matchsVars(formula);
        for (String field : matchsVars) {
            if (checkIfNeed != null && MetadataHelper.getLastJoinField(checkIfNeed, field) == null) {
                throw new MissingMetaExcetion(field, checkIfNeed.getName());
            }
            fieldVars.add(field);
        }
        return fieldVars;
    }

    /**
     * 转换公式的字段变量值
     * https://getrebuild.com/docs/topic/write-formula#%E5%AD%97%E6%AE%B5%E5%8F%98%E9%87%8F%E5%80%BC%E8%AF%B4%E6%98%8E
     *
     * @param value
     * @param field
     * @return
     */
    public static Object convertValueOfFieldVar(Object value, Field field) {
        EasyField easyField = EasyMetaFactory.valueOf(field);

        boolean isMultiField = easyField.getDisplayType() == DisplayType.MULTISELECT
                || easyField.getDisplayType() == DisplayType.TAG
                || easyField.getDisplayType() == DisplayType.N2NREFERENCE;
        boolean isStateField = easyField.getDisplayType() == DisplayType.STATE;
        boolean isNumberField = field.getType() == FieldType.LONG || field.getType() == FieldType.DECIMAL;

        if (isStateField) {
            value = value == null ? "" : StateHelper.getLabel(field, (Integer) value);
        } else if (value instanceof Date) {
            value = CalendarUtils.getUTCDateTimeFormat().format(value);
        } else if (value == null) {
            // 数字字段置 `0`
            if (isNumberField) {
                value = 0L;
            } else if (field.getType() == FieldType.REFERENCE_LIST) {
                log.debug("Keep NULL for N2N");
            } else {
                value = StringUtils.EMPTY;
            }
        } else if (isMultiField) {
            // force `TEXT`
            EasyField fakeTextField = EasyMetaFactory
                    .valueOf(MetadataHelper.getField("User", "fullName"));
            value = easyField.convertCompatibleValue(value, fakeTextField);
        } else if (value instanceof ID) {
            value = value.toString();
        }

        // v3.6.3 整数/小数强制使用 BigDecimal 高精度
        if (value instanceof Long) value = BigDecimal.valueOf((Long) value);

        return value;
    }
}
