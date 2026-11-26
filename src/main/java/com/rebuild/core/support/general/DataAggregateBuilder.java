/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合查询数据构建
 *
 * @author devezhao
 * @since 2026/10/16
 */
public class DataAggregateBuilder implements DataListBuilder {

    final protected Entity entity;
    final protected ID user;
    final protected JSONObject query;

    /**
     * @param query
     * @param user
     */
    public DataAggregateBuilder(JSONObject query, ID user) {
        this.entity = MetadataHelper.getEntity(query.getString("entity"));
        this.user = user;
        this.query = query;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    /**
     * 默认过滤条件
     *
     * @return
     */
    @Override
    public String getDefaultFilter() {
        final int entityCode = entity.getEntityCode();

        if (MetadataHelper.isBizzEntity(entityCode)) {
            List<String> where = new ArrayList<>();
            String s = UserFilters.getBizzFilter(entityCode, user);
            if (s != null) where.add(s);

            // 部门用户隔离
            s = UserFilters.getEnableBizzPartFilter(entityCode, user);
            if (s != null) where.add(s);
            return where.isEmpty() ? null : "( " + StringUtils.join(where, " and ") + " )";
        }

        return null;
    }

    @Override
    public JSON getJSONResult() {
        JSONArray aggs = wrapToArray(query.get("aggs"));
        if (aggs == null || aggs.isEmpty()) throw new DataSpecificationException("[aggs] 不能为空");

        List<String> aggFuncs = new ArrayList<>();
        List<Field> aggFields = new ArrayList<>();
        List<String> aggKeys = new ArrayList<>();
        for (int i = 0; i < aggs.size(); i++) {
            JSONObject agg = aggs.getJSONObject(i);
            String func = StringUtils.defaultString(agg.getString("func")).toUpperCase();
            if (!isValidAggFunc(func)) {
                throw new DataSpecificationException("不支持的聚合函数:" + func);
            }

            String field = agg.getString("field");
            boolean usePk = StringUtils.isBlank(field) || "*".equals(field.trim());
            Field aggField = usePk
                    ? ("COUNT".equals(func) ? entity.getPrimaryField() : null)
                    : (entity.containsField(field) ? entity.getField(field) : null);
            if (aggField == null) {
                throw new DataSpecificationException(func + " 聚合必须指定有效的聚合字段");
            }

            String typeError = validAggFieldType(func, aggField);
            if (typeError != null) throw new DataSpecificationException(typeError);

            aggFuncs.add(func);
            aggFields.add(aggField);
            aggKeys.add(StringUtils.defaultIfBlank(agg.getString("alias"),
                    usePk ? func : func + "_" + aggField.getName()));
        }

        List<Field> groupFields = new ArrayList<>();
        List<String> groupFieldNames = new ArrayList<>();
        Object groupByObj = query.get("groupBy");
        JSONArray groupBy = wrapToArray(groupByObj);
        if (groupBy == null && groupByObj != null) {
            // 支持逗号分隔的字符串
            groupBy = new JSONArray();
            for (String g : groupByObj.toString().split("[,;\\s]")) {
                if (StringUtils.isNotBlank(g)) groupBy.add(g.trim());
            }
        }
        if (groupBy != null) {
            for (Object g : groupBy) {
                Field gf = entity.containsField(g.toString()) ? entity.getField(g.toString()) : null;
                if (gf == null) throw new DataSpecificationException("无效的分组字段 : " + g);

                DisplayType gdt = EasyMetaFactory.getDisplayType(gf);
                if (gdt == DisplayType.IMAGE || gdt == DisplayType.FILE || gdt == DisplayType.NTEXT) {
                    throw new DataSpecificationException("字段 [" + EasyMetaFactory.getLabel(gf) + "] 的类型是 "
                            + gdt.getDisplayName() + "，不适合作为分组字段");
                }
                groupFields.add(gf);
                groupFieldNames.add(gf.getName());
            }
        }

        StringBuilder sql = new StringBuilder("select ");
        for (String fn : groupFieldNames) {
            sql.append(fn).append(",");
        }
        for (int i = 0; i < aggFuncs.size(); i++) {
            sql.append(aggFuncs.get(i)).append("(").append(aggFields.get(i).getName()).append(")");
            if (i < aggFuncs.size() - 1) sql.append(",");
        }
        sql.append(" from ").append(entity.getName());

        List<String> whereAnds = new ArrayList<>();

        // Default
        String defaultFilter = getDefaultFilter();
        if (StringUtils.isNotBlank(defaultFilter)) whereAnds.add(defaultFilter);

        JSONObject filter = query.getJSONObject("filter");
        if (ParseHelper.validAdvFilter(filter)) {
            String filterWhere = new AdvFilterParser(filter, entity).toSqlWhere();
            if (filterWhere != null) whereAnds.add(filterWhere);
        }
        if (!whereAnds.isEmpty()) {
            sql.append(" where ").append(StringUtils.join(whereAnds, " and "));
        }
        if (!groupFields.isEmpty()) {
            sql.append(" group by ").append(StringUtils.join(groupFieldNames, ","));
        }

        // 无分组，取单行
        if (groupFields.isEmpty()) {
            Object[] row = Application.createQuery(sql.toString(), user).unique();
            JSONObject values = new JSONObject(true);
            for (int i = 0; i < aggKeys.size(); i++) {
                values.put(aggKeys.get(i), row == null ? null : row[i]);
            }
            return JSONUtils.toJSONObject("values", values);
        }

        int limit = query.getIntValue("limit");
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Object[][] rows = Application.createQuery(sql.toString(), user).setLimit(limit).array();
        JSONArray rowsJson = new JSONArray();
        for (Object[] row : rows) {
            JSONObject item = new JSONObject(true);
            for (int i = 0; i < groupFields.size(); i++) {
                Object label = FieldValueHelper.wrapFieldValue(row[i], groupFields.get(i), true);
                item.put(groupFieldNames.get(i), label != null ? label : "(空)");
            }
            for (int i = 0; i < aggKeys.size(); i++) {
                item.put(aggKeys.get(i), row[groupFields.size() + i]);
            }
            rowsJson.add(item);
        }

        return JSONUtils.toJSONObject(
                new String[]{"rows", "groupCount"},
                new Object[]{rowsJson, rowsJson.size()});
    }

    private JSONArray wrapToArray(Object value) {
        if (value instanceof JSONArray) return (JSONArray) value;
        if (value instanceof JSONObject) {
            JSONArray array = new JSONArray();
            array.add(value);
            return array;
        }
        return null;
    }

    private String validAggFieldType(String func, Field field) {
        if ("COUNT".equals(func)) return null;

        DisplayType dt = EasyMetaFactory.getDisplayType(field);
        boolean numeric = dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL;
        boolean temporal = dt == DisplayType.DATE || dt == DisplayType.DATETIME;
        boolean allowed = numeric || (temporal && ("MAX".equals(func) || "MIN".equals(func)));
        if (!allowed) {
            return func + " 聚合要求字段为数值" + ("MAX".equals(func) || "MIN".equals(func) ? "或日期" : "")
                    + "类型，字段 [" + EasyMetaFactory.getLabel(field) + "] 的类型是 " + dt.getDisplayName();
        }
        return null;
    }

    private boolean isValidAggFunc(String func) {
        return "COUNT".equals(func) || "SUM".equals(func) || "AVG".equals(func)
                || "MAX".equals(func) || "MIN".equals(func);
    }
}
