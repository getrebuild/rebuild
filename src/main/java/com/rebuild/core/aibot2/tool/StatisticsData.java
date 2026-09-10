/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据统计/聚合工具
 *
 * @author RB
 * @since 2026/7/23
 */
@Slf4j
public class StatisticsData implements Tool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new KnownToolException("实体名称不能为空");
        }

        Entity entity = ToolHelper.resolveEntity(entityName);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }

        String aggFunc = args.getString("aggFunc");
        if (StringUtils.isBlank(aggFunc)) {
            throw new KnownToolException("聚合函数 (aggFunc) 不能为空");
        }
        aggFunc = aggFunc.toUpperCase();
        if (!isValidAggFunc(aggFunc)) {
            throw new KnownToolException("不支持的聚合函数 : " + aggFunc + "（支持 COUNT/SUM/AVG/MAX/MIN）");
        }

        String aggField = args.getString("aggField");
        String groupBy = args.getString("groupBy");
        JSONArray filter = args.getJSONArray("filter");
        String equation = args.getString("equation");
        int limit = args.getIntValue("limit");
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;

        Field aggFieldObj = resolveAggField(entity, aggFunc, aggField);
        String aggFieldSql = aggFieldObj.getName();

        String whereClause;
        try {
            whereClause = ToolHelper.parseFilterToWhere(entity, filter, equation);
        } catch (KnownToolException ex) {
            throw ex;  // parseFilterToWhere 已包装了清晰的错误消息
        } catch (Exception ex) {
            throw new KnownToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }

        if (StringUtils.isNotBlank(groupBy)) {
            return queryWithGroupBy(entity, aggFunc, aggFieldSql, groupBy, whereClause, limit);
        }

        return querySingleAgg(entity, aggFunc, aggFieldSql, aggFieldObj, whereClause);
    }

    /**
     * 无分组聚合
     */
    private JSONObject querySingleAgg(Entity entity, String aggFunc, String aggFieldSql,
                                      Field aggFieldObj, String whereClause) {
        String sql = String.format("select %s(%s) from %s",
                aggFunc, aggFieldSql, entity.getName());
        if (whereClause != null) {
            sql += " where " + whereClause;
        }

        Object[] result = Application.createQuery(sql).unique();
        Object value = (result != null && result.length > 0) ? result[0] : null;

        JSONObject ret = new JSONObject(true);
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("entityLabel", EasyMetaFactory.getLabel(entity));
        ret.put("aggFunc", aggFunc);
        ret.put("aggField", aggFieldObj.getName());
        ret.put("aggFieldLabel", EasyMetaFactory.getLabel(aggFieldObj));
        ret.put("value", value);
        // 日期原值形如 2026-09-09 00:00:00.0，附可读文本便于模型组织答复
        if (value instanceof Date) {
            ret.put("valueText", CalendarUtils.getUTCDateTimeFormat().format((Date) value));
        }
        return ret;
    }

    /**
     * 分组聚合
     */
    private JSONObject queryWithGroupBy(Entity entity, String aggFunc, String aggFieldSql,
                                        String groupBy, String whereClause, int limit) {
        // 解析分组字段（支持字段名或中文标签）
        List<Field> groupFields = new ArrayList<>();
        List<String> groupFieldNames = new ArrayList<>();
        for (String gf : groupBy.split("[,;]")) {
            gf = gf.trim();
            if (StringUtils.isBlank(gf)) continue;
            Field field = ToolHelper.resolveField(entity, gf);
            DisplayType gdt = EasyMetaFactory.valueOf(field).getDisplayType();
            if (gdt == DisplayType.IMAGE || gdt == DisplayType.FILE || gdt == DisplayType.NTEXT) {
                throw new KnownToolException("字段 [" + EasyMetaFactory.getLabel(field) + "] 的类型是 "
                        + gdt.getDisplayName() + "，不适合作为分组字段");
            }
            groupFields.add(field);
            groupFieldNames.add(field.getName());
        }

        if (groupFields.isEmpty()) {
            throw new KnownToolException("分组字段无效");
        }

        String groupFieldsSql = StringUtils.join(groupFieldNames, ",");
        String sql = String.format("select %s,%s(%s) from %s",
                groupFieldsSql, aggFunc, aggFieldSql, entity.getName());
        if (whereClause != null) {
            sql += " where " + whereClause;
        }
        sql += " group by " + groupFieldsSql;

        Object[][] results = Application.createQuery(sql).setLimit(limit).array();

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
            item.put("value", row[groupFields.size()]);
            rows.add(item);
        }

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("entityLabel", EasyMetaFactory.getLabel(entity));
        ret.put("aggFunc", aggFunc);
        ret.put("groupBy", groupFieldNames);
        // rows 已被 setLimit 截断，不能叫 total 让模型当作分组总数报告
        ret.put("groupCount", rows.size());
        if (rows.size() == limit) {
            ret.put("truncated", true);
            ret.put("message", String.format("分组数已达上限 %d，可增大 limit（最大 %d）或收窄过滤条件", limit, MAX_LIMIT));
        }
        ret.put("rows", rows);
        return ret;
    }

    /**
     * 解析并校验聚合字段。不校验类型会对文本字段做 SUM/AVG 而抛数据库层异常
     */
    private Field resolveAggField(Entity entity, String aggFunc, String aggField) {
        if ("COUNT".equals(aggFunc)) {
            if (StringUtils.isBlank(aggField) || "*".equals(aggField.trim())) {
                return entity.getPrimaryField();
            }
        }

        if (StringUtils.isBlank(aggField) || "*".equals(aggField.trim())) {
            throw new KnownToolException(aggFunc + " 聚合必须指定 aggField（数值或日期字段）。"
                    + "实体 [" + EasyMetaFactory.getLabel(entity) + "] 可用字段: " + ToolHelper.listFields(entity));
        }

        // 支持字段名或中文标签
        Field field = ToolHelper.resolveField(entity, aggField);
        if (!"COUNT".equals(aggFunc)) {
            DisplayType dt = EasyMetaFactory.valueOf(field).getDisplayType();
            boolean numeric = dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL;
            boolean temporal = dt == DisplayType.DATE || dt == DisplayType.DATETIME;
            // SUM/AVG 仅数值有意义；MAX/MIN 允许数值与日期
            boolean allowed = numeric || (temporal && ("MAX".equals(aggFunc) || "MIN".equals(aggFunc)));
            if (!allowed) {
                throw new KnownToolException(aggFunc + " 聚合要求 aggField 为"
                        + (("MAX".equals(aggFunc) || "MIN".equals(aggFunc)) ? "数值或日期" : "数值")
                        + "类型，字段 [" + EasyMetaFactory.getLabel(field) + "] 的类型是 " + dt.getDisplayName()
                        + "。实体 [" + EasyMetaFactory.getLabel(entity) + "] 可用字段: " + ToolHelper.listFields(entity));
            }
        }
        return field;
    }

    private boolean isValidAggFunc(String func) {
        return "COUNT".equals(func) || "SUM".equals(func) || "AVG".equals(func)
                || "MAX".equals(func) || "MIN".equals(func);
    }
}
