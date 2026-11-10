/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDateTime;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 表单计算公式
 *
 * @author ZHAO
 * @since 2024/8/19
 * @see AutoFillinManager
 */
@Slf4j
public class CalcFormulaSupport {

    /**
     * 是否高级公式代码
     *
     * @param formula
     * @return
     */
    public static boolean isCodeFormula(String formula) {
        if (StringUtils.isBlank(formula)) return false;
        formula = formula.trim();
        return formula.startsWith(AviatorUtils.CODE_PREFIX) && formula.endsWith(AviatorUtils.CODE_SUFFIX);
    }

    /**
     * @param field
     * @return
     */
    public static boolean isCodeFormula(Field field) {
        return isCodeFormula(EasyMetaFactory.valueOf(field).getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULA));
    }

    /**
     * 表单计算公式（启用后端计算）
     * FIXME 字段计算存在路径依赖：例如字段 B=A+1, 但 A 也是计算字段
     *
     * @param record
     */
    public static void calcFormulaBackend(Record record) {
        // 从数据库访问
        Record recordInDb = null;
        if (record.getPrimary() != null) {
            recordInDb = Application.getQueryFactory().recordNoFilter(record.getPrimary());
        }

        Field[] calcFields = MetadataSorter.sortFields(record.getEntity(),
                DisplayType.DECIMAL, DisplayType.NUMBER, DisplayType.DATE, DisplayType.DATETIME);
        for (Field field : calcFields) {
            final EasyField targetField = EasyMetaFactory.valueOf(field);
            String formula = targetField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULA);
            String backend = targetField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULABACKEND);
            if (StringUtils.isBlank(formula)) continue;
            if (!BooleanUtils.toBoolean(backend)) continue;

            final boolean useCode = isCodeFormula(formula);
            Map<String, Object> varsInFormula = new HashMap<>();
            Set<String> fieldVars = ContentWithFieldVars.matchsVars(formula);
            for (String fieldName : fieldVars) {
                Object v = record.getObjectValue(fieldName);
                if (v == null && recordInDb != null) {
                    v = recordInDb.getObjectValue(fieldName);
                }

                if (v == null && !useCode) {
                    varsInFormula = null;
                    break;
                }
                varsInFormula.put(fieldName, v);
            }
            if (varsInFormula == null) continue;

            Object evalVal = useCode
                    ? evalValue(formula, buildCodeEnv(record.getEntity(), formula, varsInFormula), targetField, false, false)
                    : evalValue(formula, varsInFormula, targetField, false, false);
            // 无值忽略
            if (evalVal == null) continue;
            // 同值忽略
            if (recordInDb != null) {
                Object dbVal = recordInDb.getObjectValue(field.getName());
                if (CommonsUtils.isSame(evalVal, dbVal)) continue;
            }

            record.setObjectValue(field.getName(), evalVal);
        }
    }

    /**
     * @param targetField
     * @param varsInFormula
     * @return
     */
    public static Object evalCalcFormula(Field targetField, Map<String, Object> varsInFormula) {
        return evalCalcFormula(targetField, varsInFormula, null, false);
    }

    /**
     * @param targetField
     * @param varsInFormula
     * @param specFormula
     * @param forceEval v4.3 （公式）强制计算结果
     * @return
     */
    public static Object evalCalcFormula(Field targetField, Map<String, Object> varsInFormula, String specFormula, boolean forceEval) {
        final Entity entity = targetField.getOwnEntity();
        final EasyField easyField = EasyMetaFactory.valueOf(targetField);
        String formula = specFormula;
        if (formula == null) formula = easyField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULA);
        if (StringUtils.isBlank(formula)) return null;

        // 高级公式代码
        if (isCodeFormula(formula)) {
            return evalValue(formula, buildCodeEnv(entity, formula, varsInFormula), easyField, true, forceEval);
        }

        // 兼容
        formula = formula.replace("{{NOW}}", EasyDateTime.VAR_NOW);

        boolean calcReady = true;
        Set<String> fieldVars = ContentWithFieldVars.matchsVars(formula);
        for (String fieldName : fieldVars) {
            if (EasyDateTime.VAR_NOW.equals(fieldName) || "NOW".equals(fieldName)) {
                varsInFormula.put(fieldName, CalendarUtils.now());
                continue;
            }

            // v4.0 支持点连接字段
            Field field = MetadataHelper.getLastJoinField(entity, fieldName);
            if (field == null) {
                calcReady = false;
                log.warn("No field found for formula : {}", formula);
                break;
            }

            Object fieldValue = varsInFormula.get(fieldName);
            if (fieldValue == null) {
                // v4.3 点连接支持
                if (fieldName.contains(".")) {
                    String fieldNameRef = fieldName.substring(0, fieldName.indexOf("."));
                    String fieldNameSub = fieldName.substring(fieldName.indexOf(".") + 1);
                    fieldValue = varsInFormula.get(fieldNameRef);
                    if (ID.isId(fieldValue) && !EntityHelper.isUnsavedId(fieldValue)) {
                        Object[] o = Application.getQueryFactory()
                                .uniqueNoFilter(ID.valueOf(fieldValue.toString()), fieldNameSub);
                        fieldValue = o == null ? null : o[0];
                    }
                }
            }
            if (fieldValue == null) {
                calcReady = false;
                varsInFormula.put(fieldName, null);

                if (forceEval) continue;
                else break;
            }

            String val2str = fieldValue.toString();
            DisplayType dt = EasyMetaFactory.getDisplayType(field);
            if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
                fieldValue = CalendarUtils.parse(val2str, CalendarUtils.UTC_DATETIME_FORMAT.substring(0, val2str.length()));
            } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
                if (StringUtils.isNotBlank(val2str)) {
                    // v3.6.3 整数/小数强制使用 BigDecimal 高精度
                    if (dt == DisplayType.NUMBER) fieldValue = BigDecimal.valueOf(ObjectUtils.toLong(fieldValue));
                    else fieldValue = BigDecimal.valueOf(ObjectUtils.toDouble(fieldValue));
                } else {
                    fieldValue = null;
                }
            }
            if (fieldValue == null) {
                calcReady = false;
                varsInFormula.put(fieldName, null);

                if (forceEval) continue;
                else break;
            }

            varsInFormula.put(fieldName, fieldValue);
        }

        return calcReady || forceEval
                ? evalValue(formula, varsInFormula, easyField, true, forceEval) : null;
    }

    /**
     * 计算公式
     *
     * @param formula
     * @param varsInFormula
     * @param targetField
     * @param wrapValue
     * @param forceEval
     * @return
     */
    protected static Object evalValue(String formula, Map<String, Object> varsInFormula, EasyField targetField, boolean wrapValue, boolean forceEval) {
        final boolean useCode = isCodeFormula(formula);
        String clearFormula;
        if (useCode) {
            String f = formula.trim();
            clearFormula = f.substring(AviatorUtils.CODE_PREFIX.length(), f.length() - AviatorUtils.CODE_SUFFIX.length());
        } else {
            clearFormula = formula.replace("×", "*").replace("÷", "/");
        }

        for (String name : varsInFormula.keySet()) {
            String replace = "{" + name + "}";
            if (useCode) {
                clearFormula = clearFormula
                        .replace("\"" + replace + "\"", name)
                        .replace("'" + replace + "'", name);
            }
            if (clearFormula.contains(replace)) {
                clearFormula = clearFormula.replace(replace, name);
            }
        }

        Object evalVal = AviatorUtils.eval(clearFormula, varsInFormula, true);
        if (AviatorUtils.isNull(evalVal)) return null;

        final DisplayType dt = targetField.getDisplayType();
        if (useCode) {
            evalVal = castCodeResult(evalVal, dt);
            if (evalVal == null) return null;
        }

        if (!wrapValue) return evalVal;

        if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            if (evalVal instanceof Date) {
                return targetField.wrapValue(evalVal);
            }
        } else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
            if (evalVal instanceof Number) {
                evalVal = targetField.wrapValue(evalVal);
                evalVal = EasyDecimal.clearFlaged(evalVal);
                return evalVal;
            }
        }

        return forceEval ? evalVal : null;
    }

    /**
     * 高级公式变量环境
     *
     * @param entity
     * @param formula
     * @param varsInFormula
     * @return
     * @see AviatorUtils#convertValueOfFieldVar(Object, Field)
     */
    private static Map<String, Object> buildCodeEnv(Entity entity, String formula, Map<String, Object> varsInFormula) {
        Map<String, Object> env = new HashMap<>();
        for (String fieldName : ContentWithFieldVars.matchsVars(formula)) {
            if ("NOW".equals(fieldName)) {
                env.put(fieldName, CalendarUtils.now());
                continue;
            }

            Field field = MetadataHelper.getLastJoinField(entity, fieldName);
            Object value = varsInFormula == null ? null : varsInFormula.get(fieldName);
            if (field == null) {
                env.put(fieldName, value);
                continue;
            }

            // 表单实时计算/回填公式传入的数字值为字符串，需转换为数值（Aviator 不做字符串隐式转换）
            if (value instanceof String && (field.getType() == FieldType.LONG || field.getType() == FieldType.DECIMAL)) {
                String val2str = (String) value;
                // v3.6.3 整数/小数强制使用 BigDecimal 高精度
                if (StringUtils.isBlank(val2str)) value = null;
                else if (field.getType() == FieldType.LONG) value = BigDecimal.valueOf(ObjectUtils.toLong(val2str));
                else value = BigDecimal.valueOf(ObjectUtils.toDouble(val2str));
            }
            env.put(fieldName, AviatorUtils.convertValueOfFieldVar(value, field));
        }
        return env;
    }

    /**
     * 高级公式结果转换为目标字段类型
     *
     * @param evalVal
     * @param targetType
     * @return
     */
    private static Object castCodeResult(Object evalVal, DisplayType targetType) {
        if (targetType == DisplayType.DATE || targetType == DisplayType.DATETIME) {
            return evalVal instanceof Date ? evalVal : CalendarUtils.parse(evalVal.toString());
        }

        if (targetType == DisplayType.NUMBER || targetType == DisplayType.DECIMAL) {
            if (evalVal instanceof Number) return evalVal;
            try {
                return new BigDecimal(evalVal.toString().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
