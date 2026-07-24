/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据统计/聚合工具。支持 COUNT/SUM/AVG/MAX/MIN + GROUP BY + 条件过滤
 *
 * @author RB
 * @since 2026/7/23
 */
@Slf4j
public class DataStatistics implements Tool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName);
        }

        String aggFunc = args.getString("aggFunc");
        if (StringUtils.isBlank(aggFunc)) {
            throw new ToolException("聚合函数 (aggFunc) 不能为空");
        }
        aggFunc = aggFunc.toUpperCase();
        if (!isValidAggFunc(aggFunc)) {
            throw new ToolException("不支持的聚合函数 : " + aggFunc + "（支持 COUNT/SUM/AVG/MAX/MIN）");
        }

        String aggField = args.getString("aggField");
        String groupBy = args.getString("groupBy");
        JSONArray filter = args.getJSONArray("filter");
        int limit = args.getIntValue("limit");
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;

        // 校验聚合字段
        String aggFieldSql = buildAggFieldSql(entity, aggFunc, aggField);

        // 构建 WHERE
        String whereClause = buildWhereClause(entity, filter);

        // 有分组
        if (StringUtils.isNotBlank(groupBy)) {
            return queryWithGroupBy(entity, aggFunc, aggFieldSql, groupBy, whereClause, limit);
        }

        // 无分组，返回单一聚合值
        return querySingleAgg(entity, aggFunc, aggFieldSql, whereClause);
    }

    /**
     * 无分组聚合
     */
    private JSONObject querySingleAgg(Entity entity, String aggFunc, String aggFieldSql, String whereClause) {
        String sql = String.format("select %s(%s) from %s",
                aggFunc, aggFieldSql, entity.getName());
        if (whereClause != null) {
            sql += " where " + whereClause;
        }

        Object[] result = Application.createQueryNoFilter(sql).unique();
        Object value = (result != null && result.length > 0) ? result[0] : null;

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("aggFunc", aggFunc);
        ret.put("value", value);
        return ret;
    }

    /**
     * 分组聚合
     */
    private JSONObject queryWithGroupBy(Entity entity, String aggFunc, String aggFieldSql,
                                        String groupBy, String whereClause, int limit) {
        // 解析分组字段
        List<Field> groupFields = new ArrayList<>();
        List<String> groupFieldNames = new ArrayList<>();
        for (String gf : groupBy.split("[,;]")) {
            gf = gf.trim();
            if (StringUtils.isBlank(gf)) continue;
            if (!entity.containsField(gf)) {
                throw new ToolException("分组字段不存在 : " + gf);
            }
            groupFields.add(entity.getField(gf));
            groupFieldNames.add(gf);
        }

        if (groupFields.isEmpty()) {
            throw new ToolException("分组字段无效");
        }

        String groupFieldsSql = StringUtils.join(groupFieldNames, ",");
        String sql = String.format("select %s,%s(%s) from %s",
                groupFieldsSql, aggFunc, aggFieldSql, entity.getName());
        if (whereClause != null) {
            sql += " where " + whereClause;
        }
        sql += " group by " + groupFieldsSql;
        sql += " order by 2 desc";  // 按聚合值降序

        Object[][] results = Application.createQueryNoFilter(sql).setLimit(limit).array();

        JSONArray rows = new JSONArray();
        for (Object[] row : results) {
            JSONObject item = new JSONObject(true);
            // 分组字段值（转为可读标签）
            for (int i = 0; i < groupFields.size(); i++) {
                Field gf = groupFields.get(i);
                Object rawValue = row[i];
                Object label = FieldValueHelper.wrapFieldValue(rawValue, gf, true);
                item.put(gf.getName(), label != null ? label : "(空)");
            }
            // 聚合值
            item.put("value", row[groupFields.size()]);
            rows.add(item);
        }

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("aggFunc", aggFunc);
        ret.put("groupBy", groupFieldNames);
        ret.put("total", rows.size());
        ret.put("rows", rows);
        return ret;
    }

    /**
     * 构建聚合字段 SQL 片段
     */
    private String buildAggFieldSql(Entity entity, String aggFunc, String aggField) {
        if ("COUNT".equals(aggFunc)) {
            if (StringUtils.isBlank(aggField) || "*".equals(aggField.trim())) {
                return entity.getPrimaryField().getName();
            }
        }

        if (StringUtils.isBlank(aggField) || "*".equals(aggField.trim())) {
            throw new ToolException(aggFunc + " 聚合必须指定 aggField（数值或日期字段）");
        }

        if (!entity.containsField(aggField)) {
            throw new ToolException("聚合字段不存在 : " + aggField);
        }

        return aggField;
    }

    /**
     * 构建 WHERE 子句（复用 AdvFilterParser）
     */
    private String buildWhereClause(Entity entity, JSONArray filter) {
        if (filter == null || filter.isEmpty()) return null;

        JSONObject filterExpr = new JSONObject();
        filterExpr.put("entity", entity.getName());
        filterExpr.put("items", filter);

        try {
            return new AdvFilterParser(filterExpr, entity).toSqlWhere();
        } catch (Exception ex) {
            throw new ToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }
    }

    private boolean isValidAggFunc(String func) {
        return "COUNT".equals(func) || "SUM".equals(func) || "AVG".equals(func)
                || "MAX".equals(func) || "MIN".equals(func);
    }
}
