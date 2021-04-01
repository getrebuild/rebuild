/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 归集计算
 *
 * @author devezhao
 * @since 2020/1/16
 */
@Slf4j
public class AggregationEvaluator {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.newInstance();
    static {
        // 强制使用 BigDecimal/BigInteger 运算
        AVIATOR.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, true);
    }

    /**
     * 算数计算
     *
     * @param formual
     * @return
     */
    protected static Object calc(String formual) {
        try {
            return AVIATOR.execute(formual);
        } catch (ExpressionSyntaxErrorException ex) {
            log.error("Bad formula : {}", formual, ex);
            return null;
        }
    }

    // --

    final private Entity sourceEntity;
    final private JSONObject item;
    final private String followSourceField;
    final private String filterSql;

    /**
     * @param item
     * @param sourceEntity
     * @param followSourceField
     * @param filterSql
     */
    protected AggregationEvaluator(JSONObject item, Entity sourceEntity, String followSourceField, String filterSql) {
        this.sourceEntity = sourceEntity;
        this.item = item;
        this.followSourceField = followSourceField;
        this.filterSql = filterSql;
    }

    /**
     * @param triggerRecord
     * @return
     */
    public Object eval(ID triggerRecord) {
        String calcMode = item.getString("calcMode");
        if ("FORMULA".equalsIgnoreCase(calcMode)) {
            return evalFormula(triggerRecord);
        }

        String sourceField = item.getString("sourceField");
        if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
            return null;
        }

        String funcAndField = String.format("%s(%s)", calcMode, sourceField);
        // 去重计数
        if ("COUNT2".equalsIgnoreCase(calcMode)) {
            funcAndField = String.format("COUNT(DISTINCT %s)", sourceField);
        }

        String sql = String.format("select %s from %s where %s = ?",
                funcAndField, sourceEntity.getName(), followSourceField);
        if (filterSql != null) {
            sql += " and " + filterSql;
        }

        Object[] o = Application.createQueryNoFilter(sql)
                .setParameter(1, triggerRecord)
                .unique();
        return o == null || o[0] == null ? 0 : o[0];
    }

    /**
     * @param triggerRecord
     * @return
     */
    private Object evalFormula(ID triggerRecord) {
        String formula = item.getString("sourceFormula");
        Matcher m = FieldAggregation.PATT_FIELD.matcher(formula);

        final List<String[]> fields = new ArrayList<>();
        while (m.find()) {
            String[] fieldAndFunc = m.group(1).split("\\$\\$\\$\\$");
            if (MetadataHelper.getLastJoinField(sourceEntity, fieldAndFunc[0]) != null) {
                fields.add(fieldAndFunc);
            }
        }
        if (fields.isEmpty()) return null;

        StringBuilder sql = new StringBuilder("select ");
        for (String[] field : fields) {
            if (field.length == 2) {
                sql.append(String.format("%s(%s)", field[1], field[0]));
            } else {
                sql.append(field[0]);
            }
            sql.append(',');
        }
        sql.deleteCharAt(sql.length() - 1)
                .append(" from ").append(sourceEntity.getName())
                .append(" where ").append(followSourceField).append(" = ?");
        if (filterSql != null) {
            sql.append(" and ").append(filterSql);
        }

        Object[] o = Application.createQueryNoFilter(sql.toString())
                .setParameter(1, triggerRecord)
                .unique();
        if (o == null) {
            return null;
        }

        String newFormual = formula.toUpperCase()
                .replace("×", "*")
                .replace("÷", "/");
        for (int i = 0; i < fields.size(); i++) {
            String[] field = fields.get(i);
            Object v = o[i] == null ? "0" : o[i];
            String replace = "{" + StringUtils.join(field, "$$$$") + "}";
            newFormual = newFormual.replace(replace.toUpperCase(), v.toString());
        }

        return calc(newFormual);
    }
}
