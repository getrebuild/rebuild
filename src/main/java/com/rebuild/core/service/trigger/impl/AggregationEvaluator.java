/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.ContentWithFieldVars;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 聚合计算
 *
 * @author devezhao
 * @since 2020/1/16
 */
@Slf4j
public class AggregationEvaluator {

    final private Entity sourceEntity;
    final private JSONObject item;
    final private String filterSql;

    /**
     * @param item
     * @param sourceEntity
     * @param filterSql
     */
    protected AggregationEvaluator(JSONObject item, Entity sourceEntity, String filterSql) {
        this.sourceEntity = sourceEntity;
        this.item = item;
        this.filterSql = filterSql;
    }

    /**
     * 计算
     *
     * @return
     */
    public Object eval() {
        String calcMode = item.getString("calcMode");
        if ("FORMULA".equalsIgnoreCase(calcMode)) {
            return evalFormula();
        }

        String sourceField = item.getString("sourceField");
        if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
            throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
        }

        String funcAndField = String.format("%s(%s)", calcMode, sourceField);
        // 去重计数
        if ("COUNT2".equalsIgnoreCase(calcMode)) {
            funcAndField = String.format("COUNT(DISTINCT %s)", sourceField);
        }

        String ql = String.format("select %s from %s where %s", funcAndField, sourceEntity.getName(), filterSql);
        Object[] o = Application.createQueryNoFilter(ql).unique();
        return o == null || o[0] == null ? 0 : o[0];
    }

    /**
     * 计算公式
     *
     * @return
     */
    private Object evalFormula() {
        String formula = item.getString("sourceFormula");
        Set<String> matchsVars = ContentWithFieldVars.matchsVars(formula);

        List<String[]> fields = new ArrayList<>();
        for (String m : matchsVars) {
            String[] fieldAndFunc = m.split(MetadataHelper.SPLITER2);
            if (MetadataHelper.getLastJoinField(sourceEntity, fieldAndFunc[0]) == null) {
                throw new MissingMetaExcetion(fieldAndFunc[0], sourceEntity.getName());
            }
            fields.add(fieldAndFunc);
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
                .append(" where ").append(filterSql);

        final Object[] useSourceData = Application.createQueryNoFilter(sql.toString()).unique();
        if (useSourceData == null) return null;

        String clearFormual = formula.toUpperCase()
                .replace("×", "*")
                .replace("÷", "/");

        for (int i = 0; i < fields.size(); i++) {
            String[] field = fields.get(i);
            Object value = useSourceData[i] == null ? "0" : useSourceData[i];

            String replace = "{" + StringUtils.join(field, MetadataHelper.SPLITER) + "}";
            clearFormual = clearFormual.replace(replace.toUpperCase(), value.toString());
        }

        return EvaluatorUtils.eval(clearFormual);
    }
}
