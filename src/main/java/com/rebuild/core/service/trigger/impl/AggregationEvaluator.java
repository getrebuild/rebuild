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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.aviator.AviatorUtils;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.state.StateHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rebuild.core.service.trigger.aviator.AviatorUtils.CODE_PREFIX;

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
        // for FieldAggregation/GroupAggregation
        else if ("RBJOIN".equalsIgnoreCase(calcMode)
                || "RBJOIN2".equalsIgnoreCase(calcMode) || "RBJOIN3".equalsIgnoreCase(calcMode)) {
            int mode = "RBJOIN2".equalsIgnoreCase(calcMode) ? 2 : ("RBJOIN3".equalsIgnoreCase(calcMode) ? 3 : 1);
            return evalRbJoin(mode);
        }

        String sourceField = item.getString("sourceField");
        if (MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
            throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
        }

        String filterSql2 = filterSql;
        String funcAndField = String.format("%s(%s)", calcMode, sourceField);
        // 去重计数
        if ("COUNT2".equalsIgnoreCase(calcMode)) {
            funcAndField = String.format("COUNT(DISTINCT %s)", sourceField);
        }
        // v4.2 随机赋值
        else if ("RAND".equals(calcMode)) {
            funcAndField = sourceField;
            filterSql2 += String.format(" and (%s is not null)", sourceField);
        }

        String ql = String.format("select %s from %s where %s", funcAndField, sourceEntity.getName(), filterSql2);
        Object[] o = Application.createQueryNoFilter(ql).unique();
        return o == null || o[0] == null ? 0 : o[0];
    }

    /**
     * 执行计算公式
     *
     * @return
     * @see AviatorUtils#convertValueOfFieldVar(Object, Field)
     */
    public Object evalFormula() {
        String formula = item.getString("sourceFormula");
        // 高级公式代码
        boolean useCode43 = formula.startsWith(CODE_PREFIX);
        if (useCode43) {
            formula = formula.substring(4, formula.length() - 4);
        }

        Set<String> matchsVars = AviatorUtils.matchsFieldVars(formula, null);

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
            log.warn("No any fields found in formula : {}", formula);
            fields.add(new String[]{sourceEntity.getPrimaryField().getName()});
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

        Record useSourceData = Application.createQueryNoFilter(sql.toString()).record();
        if (useSourceData == null) {
            log.warn("No record found by sql : {}", sql);
            return null;
        }

        String clearFormula = useCode43 ? formula
                : formula.replace("×", "*").replace("÷", "/");
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

            String fieldName = fieldAndFunc[0];
            Object value = useSourceData.getObjectValue(fieldName);

            if (useCode43) {
                Field useVarField = MetadataHelper.getLastJoinField(sourceEntity, fieldName);

                // @see AviatorUtils#convertValueOfFieldVar(Object, Field)
                EasyField easyVarField = null;
                boolean isMultiField = false;
                boolean isStateField = false;
                boolean isNumberField = false;
                if (useVarField != null) {
                    easyVarField = EasyMetaFactory.valueOf(useVarField);
                    isMultiField = easyVarField.getDisplayType() == DisplayType.MULTISELECT
                            || easyVarField.getDisplayType() == DisplayType.TAG
                            || easyVarField.getDisplayType() == DisplayType.N2NREFERENCE;
                    isStateField = easyVarField.getDisplayType() == DisplayType.STATE;
                    isNumberField = useVarField.getType() == FieldType.LONG || useVarField.getType() == FieldType.DECIMAL;
                }

                if (isStateField) {
                    value = value == null ? "" : StateHelper.getLabel(useVarField, (Integer) value);
                } else if (value instanceof Date) {
                    value = CalendarUtils.getUTCDateTimeFormat().format(value);
                } else if (value == null) {
                    // 数字字段置 `0`
                    if (isNumberField) {
                        value = 0L;
                    } else {
                        value = StringUtils.EMPTY;
                    }
                } else if (isMultiField) {
                    // force `TEXT`
                    EasyField fakeTextField = EasyMetaFactory
                            .valueOf(MetadataHelper.getField("User", "fullName"));
                    value = easyVarField.convertCompatibleValue(value, fakeTextField);
                } else if (value instanceof ID) {
                    value = value.toString();
                }

                // v3.6.3 整数/小数强制使用 BigDecimal 高精度
                if (value instanceof Long) value = BigDecimal.valueOf((Long) value);

            } else {

                if (n2nFields.contains(fieldKey)) value = new Object[0];
                else if (value == null) value = numFields.contains(fieldKey) ? 0 : StringUtils.EMPTY;
                else if (value instanceof Date) value = CalendarUtils.getUTCDateTimeFormat().format(value);
            }

            envMap.put(fieldKey, value);
        }

        return AviatorUtils.eval(clearFormula, envMap, false);
    }

    /**
     * 智能连接
     *
     * @param mode 去重模式
     * @return
     */
    private Object evalRbJoin(int mode) {
        String sourceField = item.getString("sourceField");
        Field field;
        if ((field = MetadataHelper.getLastJoinField(sourceEntity, sourceField)) == null) {
            throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
        }

        String ql = String.format("select %s,%s from %s where %s",
                sourceField, sourceEntity.getPrimaryField().getName(), sourceEntity.getName(), filterSql);
        Object[][] array = Application.createQueryNoFilter(ql).array();
        if (array.length == 0) return new Object[0];

        EasyField easyField = EasyMetaFactory.valueOf(field);

        Collection<Object> nvList;
        Map<Object, Integer> countList = null;
        if (mode == 2 || mode == 3) {
            nvList = new LinkedHashSet<>();
            if (mode == 3) countList = new HashMap<>();  // 仅文本有效 *N
        } else {
            nvList = new ArrayList<>();
        }

        for (Object[] o : array) {
            Object n = o[0];
            if (n == null) continue;

            // *N
            boolean xN3 = false;
            if (n instanceof ID && mode == 3) {
                n = FieldValueHelper.getLabel((ID) n, StringUtils.EMPTY);
                xN3 = true;
            }

            // 多引用
            if (n instanceof ID[]) {
                CollectionUtils.addAll(nvList, (ID[]) n);
            } else if (n instanceof ID) {
                if (field.getType() == FieldType.PRIMARY) {
                    nvList.add(n.toString());  // 保持主键为文本
                } else {
                    nvList.add(n);
                }
            } else {
                Object v = xN3 ? n : easyField.wrapValue(n);
                if (v == null) continue;

                DisplayType dt = easyField.getDisplayType();
                if (dt == DisplayType.MULTISELECT) {
                    JSONArray a = ((JSONObject) v).getJSONArray("text");
                    CollectionUtils.addAll(nvList, a);

                    // TEXT*N
                    if (countList != null) {
                        for (Object item : a) {
                            Integer c = countList.get(item);
                            if (c == null) c = 0;
                            countList.put(item, ++c);
                        }
                    }
                } else if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) {
                    nvList.addAll((JSONArray) v);
                } else {
                    // TEXT
                    nvList.add(v);

                    // TEXT*N
                    if (countList != null) {
                        Integer c = countList.get(v);
                        if (c == null) c = 0;
                        countList.put(v, ++c);
                    }
                }
            }
        }

        if (countList == null || countList.isEmpty()) {
            // Use array
            return nvList.toArray(new Object[0]);
        }

        Collection<Object> nvList2 = new LinkedHashSet<>();
        for (Object v : nvList) {
            nvList2.add(v + "*" + countList.getOrDefault(v, 1));
        }
        // Use array
        return nvList2.toArray(new Object[0]);
    }
}
