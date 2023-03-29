/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.core.support.general.ContentWithFieldVars;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public AggregationEvaluator(JSONObject item, Entity sourceEntity, String filterSql) {
        this.sourceEntity = sourceEntity;
        this.item = item;
        this.filterSql = filterSql;
    }

    /**
     * 执行计算
     *
     * @return
     */
    public Object eval() {
        String calcMode = item.getString("calcMode");
        if ("FORMULA".equalsIgnoreCase(calcMode)) {
            return evalFormula();
        }
        // ONLY FieldAggregation
        else if ("RBJOIN".equalsIgnoreCase(calcMode)) {
            return evalRbJoin();
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
     * 执行计算公式
     *
     * @return
     */
    public Object evalFormula() {
        String formula = item.getString("sourceFormula");
        Set<String> matchsVars = ContentWithFieldVars.matchsVars(formula);

        List<String[]> fields = new ArrayList<>();
        List<String[]> fields4Sql = new ArrayList<>();

        Set<String> n2nFields = new HashSet<>();
        Set<String> numFields = new HashSet<>();

        for (String m : matchsVars) {
            String[] fieldAndFunc = m.split(MetadataHelper.SPLITER_RE);
            Field field;
            boolean n2nField = false;
            if ((field = MetadataHelper.getLastJoinField(sourceEntity, fieldAndFunc[0])) == null) {
                // v3.3 N2N
                if ((field = MetadataHelper.getLastJoinField(sourceEntity, fieldAndFunc[0], true)) == null) {
                    throw new MissingMetaExcetion(fieldAndFunc[0], sourceEntity.getName());
                } else {
                    n2nField = true;
                }
            }

            fields.add(fieldAndFunc);

            if (n2nField) {
                n2nFields.add(StringUtils.join(fieldAndFunc, "_"));
            } else {
                fields4Sql.add(fieldAndFunc);
            }

            if (fieldAndFunc.length > 1 || field.getType() == FieldType.LONG || field.getType() == FieldType.DECIMAL) {
                numFields.add(StringUtils.join(fieldAndFunc, "_"));
            }
        }

        if (fields.isEmpty()) {
            log.warn("No fields found in formula : {}", formula);
            fields.add(new String[] { sourceEntity.getPrimaryField().getName() });
        }

        StringBuilder sql = new StringBuilder("select ");
        for (String[] fieldAndFunc : fields4Sql) {
            if (fieldAndFunc.length == 2) {
                sql.append(String.format("%s(%s)", fieldAndFunc[1], fieldAndFunc[0]));
            } else {
                sql.append(fieldAndFunc[0]);
            }
            sql.append(',');
        }
        sql.append(sourceEntity.getPrimaryField().getName())
                .append(" from ").append(sourceEntity.getName())
                .append(" where ").append(filterSql);

        final Record useSourceData = Application.createQueryNoFilter(sql.toString()).record();
        if (useSourceData == null) {
            log.warn("No record found by sql : {}", sql);
            return null;
        }

        String clearFormula = formula
                .replace("×", "*")
                .replace("÷", "/");

        Map<String, Object> envMap = new HashMap<>();

        for (String[] fieldAndFunc : fields) {
            String fieldKey = StringUtils.join(fieldAndFunc, "_");

            String replace = "{" + StringUtils.join(fieldAndFunc, MetadataHelper.SPLITER) + "}";
            String replaceWhitQuote = "\"" + replace + "\"";
            String replaceWhitQuoteSingle = "'" + replace + "'";

            if (clearFormula.contains(replaceWhitQuote)) {
                clearFormula = clearFormula.replace(replaceWhitQuote, fieldKey);
            } else if (clearFormula.contains(replaceWhitQuoteSingle)) {
                clearFormula = clearFormula.replace(replaceWhitQuoteSingle, fieldKey);
            } else if (clearFormula.contains(replace)) {
                clearFormula = clearFormula.replace(replace, fieldKey);
            } else {
                continue;
            }

            Object value = useSourceData.getObjectValue(fieldAndFunc[0]);

            if (n2nFields.contains(fieldKey)) value = new Object[0];
            else if (value == null) value = numFields.contains(fieldKey) ? 0 : StringUtils.EMPTY;
            else if (value instanceof Date) value = CalendarUtils.getUTCDateTimeFormat().format(value);

            envMap.put(fieldKey, value);
        }

        return AviatorUtils.eval(clearFormula, envMap, false);
    }

    /**
     * 智能连接
     *
     * @return
     */
    private Object evalRbJoin() {
        String sourceField = item.getString("sourceField");
        Field field;
        if ((field = MetadataHelper.getLastJoinField(sourceEntity, sourceField)) == null) {
            throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
        }

        String ql = String.format("select %s,%s from %s where %s",
                sourceField, sourceEntity.getPrimaryField().getName(), sourceEntity.getName(), filterSql);
        Object[][] array = Application.createQueryNoFilter(ql).array();

        EasyField easyField = EasyMetaFactory.valueOf(field);
        List<Object> nvList = new ArrayList<>();
        for (Object[] o : array) {
            Object n = o[0];
            if (n == null) continue;

            if (n.getClass().isArray()) {  // ID[]
                CollectionUtils.addAll(nvList, (ID[]) n);
            } else if (n instanceof ID) {
                nvList.add(n);
            } else {
                nvList.add(easyField.wrapValue(n));
            }
        }

        // Use array
        return nvList.toArray(new Object[0]);
    }
}
