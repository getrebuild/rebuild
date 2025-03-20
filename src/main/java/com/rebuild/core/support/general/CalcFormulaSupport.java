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
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.*;
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
     * 表单计算公式，后端计算。
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

        for (Field field
                : MetadataSorter.sortFields(record.getEntity(), DisplayType.DECIMAL, DisplayType.NUMBER, DisplayType.DATE, DisplayType.DATETIME)) {
            final EasyField targetField = EasyMetaFactory.valueOf(field);
            String formula = targetField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULA);
            String backend = targetField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULABACKEND);
            if (StringUtils.isBlank(formula)) continue;
            if (!BooleanUtils.toBoolean(backend)) continue;

            Map<String, Object> varsInFormula = new HashMap<>();
            Set<String> fieldVars = ContentWithFieldVars.matchsVars(formula);
            for (String fieldName : fieldVars) {
                Object v = record.getObjectValue(fieldName);
                if (v == null && recordInDb != null) {
                    v = recordInDb.getObjectValue(fieldName);
                }

                if (v == null) {
                    varsInFormula = null;
                    break;
                }
                varsInFormula.put(fieldName, v);
            }
            if (varsInFormula == null) continue;

            Object evalVal = evalValue(formula, varsInFormula, targetField, false);
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
     * 计算
     *
     * @param targetField
     * @param varsInFormula
     * @return
     */
    public static Object evalCalcFormula(Field targetField, Map<String, Object> varsInFormula) {
        return evalCalcFormula(targetField, varsInFormula, null);
    }

    /**
     * 计算
     *
     * @param targetField
     * @param varsInFormula
     * @param specFormula
     * @return
     */
    public static Object evalCalcFormula(Field targetField, Map<String, Object> varsInFormula, String specFormula) {
        final Entity entity = targetField.getOwnEntity();
        final EasyField easyField = EasyMetaFactory.valueOf(targetField);
        String formula = specFormula;
        if (formula == null) formula = easyField.getExtraAttr(EasyFieldConfigProps.NUMBER_CALCFORMULA);
        formula = formula.replace("{{NOW}}", EasyDateTime.VAR_NOW);

        boolean calcReady = true;
        Set<String> fieldVars = ContentWithFieldVars.matchsVars(formula);
        for (String fieldName : fieldVars) {
            if (EasyDateTime.VAR_NOW.equals(fieldName) || "NOW".equals(fieldName)) {
                varsInFormula.put(fieldName, CalendarUtils.now());
                continue;
            }

            // v40 支持点连接字段
            Field field = MetadataHelper.getLastJoinField(entity, fieldName);
            if (field == null) {
                calcReady = false;
                break;
            }

            Object fieldValue = varsInFormula.get(fieldName);
            if (fieldValue == null) {
                calcReady = false;
                break;
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
                break;
            }
            varsInFormula.put(fieldName, fieldValue);
        }

        return calcReady ? evalValue(formula, varsInFormula, easyField, true) : null;
    }

    /**
     * 执行计算
     *
     * @param formula
     * @param varsInFormula
     * @param targetField
     * @param wrapValue
     * @return
     */
    private static Object evalValue(String formula, Map<String, Object> varsInFormula, EasyField targetField, boolean wrapValue) {
        String clearFormula = formula
                .replace("{", "").replace("}", "")
                .replace("×", "*").replace("÷", "/");

        Object evalVal = AviatorUtils.eval(clearFormula, varsInFormula, true);
        if (evalVal == null) return null;
        if (!wrapValue) return evalVal;

        DisplayType dt = targetField.getDisplayType();
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

        return null;
    }
}
