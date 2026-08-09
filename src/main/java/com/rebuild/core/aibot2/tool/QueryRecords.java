/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 查询指定实体的记录数据。支持按名称/编号模糊匹配，也可返回记录列表
 *
 * @author devezhao
 * @since 2026/7/20
 */
@Slf4j
public class QueryRecords implements Tool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }

        String name = args.getString("name");
        JSONArray filter = args.getJSONArray("filter");
        String equation = args.getString("equation");
        String fields = args.getString("fields");
        String sort = args.getString("sort");
        int limit = args.getIntValue("limit");
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        int pageNo = args.getIntValue("pageNo");
        if (pageNo < 1) pageNo = 1;

        // 构建查询字段列表（同时收集无效字段名）
        JSONArray invalidFields = new JSONArray();
        List<String> queryFields = buildQueryFields(entity, fields, invalidFields);
        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        // 构建排序子句
        String orderBy = buildOrderBy(entity, sort);
        // 分页偏移量
        int offset = (pageNo - 1) * limit;

        JSONObject result;
        // 按名称/编号模糊匹配
        if (StringUtils.isNotBlank(name)) {
            result = queryByName(entity, primaryField, nameField, queryFields, name, limit, offset, orderBy);
        } else if (filter != null && !filter.isEmpty()) {
            // 按字段条件过滤
            result = queryByFilter(entity, primaryField, nameField, queryFields, filter, equation, limit, offset, orderBy);
        } else {
            // 返回记录列表
            result = queryList(entity, primaryField, nameField, queryFields, limit, offset, orderBy);
        }

        if (!invalidFields.isEmpty()) {
            result.put("invalidFields", invalidFields);
        }
        return result;
    }

    /**
     * 按名称/编号模糊匹配（优先使用系统配置的快速查询字段，未配置则使用名称字段 + SERIES 字段）
     */
    private JSONObject queryByName(Entity entity, Field primaryField, Field nameField,
                                   List<String> queryFields, String name, int limit, int offset, String orderBy) {
        // 优先使用系统配置的快速查询字段
        Set<String> searchFields = ParseHelper.buildQuickFields(entity, null);

        // 未配置快速查询字段时，使用名称字段 + SERIES 字段作为 fallback
        if (searchFields.isEmpty()) {
            if (nameField != null) {
                searchFields.add(nameField.getName());
            }
            for (Field f : entity.getFields()) {
                if (MetadataHelper.isSystemField(f)) continue;
                if (EasyMetaFactory.getDisplayType(f) == DisplayType.SERIES) {
                    searchFields.add(f.getName());
                }
            }
        }

        if (searchFields.isEmpty()) {
            throw new ToolException("该实体没有可搜索的名称或编号字段");
        }

        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        // 转义 LIKE 通配符防注入，同时处理单引号
        String escapedName = name.replace("'", "''").replace("%", "\\%").replace("_", "\\_");
        String likeValue = "'%" + escapedName + "%'";
        StringBuilder whereClause = new StringBuilder();
        int i = 0;
        for (String fieldName : searchFields) {
            if (i > 0) whereClause.append(" or ");
            whereClause.append(fieldName).append(" like ").append(likeValue);
            i++;
        }

        String sql = String.format("select %s from %s where %s%s",
                fieldsSql, entity.getName(), whereClause, orderBy);

        Object[][] results = Application.createQuery(sql).setLimit(limit, offset).array();
        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return buildResult(entity, records, limit, offset, whereClause.toString());
    }

    /**
     * 按字段条件过滤查询（使用 AdvFilterParser 解析条件）
     */
    private JSONObject queryByFilter(Entity entity, Field primaryField, Field nameField,
                                     List<String> queryFields, JSONArray filter, String equation, int limit, int offset, String orderBy) {
        String whereClause;
        try {
            whereClause = ToolHelper.parseFilterToWhere(entity, filter, equation);
        } catch (Exception ex) {
            throw new ToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }

        if (StringUtils.isBlank(whereClause)) {
            throw new ToolException("过滤条件无效，请检查字段名和操作符是否正确");
        }

        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s where %s%s",
                fieldsSql, entity.getName(), whereClause, orderBy);

        Object[][] results = Application.createQuery(sql).setLimit(limit, offset).array();
        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return buildResult(entity, records, limit, offset, whereClause);
    }

    /**
     * 返回记录列表
     */
    private JSONObject queryList(Entity entity, Field primaryField, Field nameField,
                                List<String> queryFields, int limit, int offset, String orderBy) {
        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s%s", fieldsSql, entity.getName(), orderBy);

        Object[][] results = Application.createQuery(sql)
                .setLimit(limit, offset)
                .array();

        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return buildResult(entity, records, limit, offset, null);
    }

    /**
     * 构建排序子句。格式：字段名:asc 或 字段名:desc，默认 modifiedOn:desc
     */
    private String buildOrderBy(Entity entity, String sort) {
        if (StringUtils.isBlank(sort)) {
            return entity.containsField("modifiedOn") ? " order by modifiedOn desc" : "";
        }

        String[] parts = sort.split(":");
        String sortField = parts[0].trim();
        String direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? "asc" : "desc";

        if (!entity.containsField(sortField)) {
            throw new ToolException("排序字段不存在 : " + sortField + ToolHelper.suggestField(entity, sortField));
        }

        return " order by " + sortField + " " + direction;
    }

    /**
     * 构建查询字段列表（不含主键和名称字段，它们会被单独添加）
     */
    private List<String> buildQueryFields(Entity entity, String fields, JSONArray invalidFields) {
        Set<String> result = new LinkedHashSet<>();
        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        if (StringUtils.isBlank(fields)) {
            // 默认返回所有非系统、可查询的字段
            for (Field f : entity.getFields()) {
                if (MetadataHelper.isSystemField(f)) continue;
                if (f.getType() == cn.devezhao.persist4j.dialect.FieldType.PRIMARY) continue;
                if (!EasyMetaFactory.valueOf(f).isQueryable()) continue;
                String fn = f.getName();
                if (fn.equals(primaryField.getName())) continue;
                if (nameField != null && fn.equals(nameField.getName())) continue;
                result.add(fn);
            }
        } else {
            for (String f : fields.split("[,;]")) {
                f = f.trim();
                if (StringUtils.isBlank(f)) continue;
                if (!entity.containsField(f)) {
                    if (invalidFields != null) {
                        JSONObject invalid = new JSONObject();
                        invalid.put("name", f);
                        String suggestion = ToolHelper.suggestField(entity, f);
                        if (StringUtils.isNotBlank(suggestion)) invalid.put("suggestion", suggestion);
                        invalidFields.add(invalid);
                    }
                    continue;
                }
                if (f.equals(primaryField.getName())) continue;
                if (nameField != null && f.equals(nameField.getName())) continue;
                result.add(f);
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 构建 SQL 字段列表（主键 + 名称字段 + 查询字段）
     *
     * @param primaryField
     * @param nameField
     * @param queryFields
     * @return
     */
    private String buildFieldsSql(Field primaryField, Field nameField, List<String> queryFields) {
        List<String> fields = new ArrayList<>();
        fields.add(primaryField.getName());
        if (nameField != null && !nameField.getName().equals(primaryField.getName())) {
            fields.add(nameField.getName());
        }
        fields.addAll(queryFields);
        return StringUtils.join(fields, ",");
    }

    /**
     * 构建统一响应结果，包含真实总数和分页信息
     */
    private JSONObject buildResult(Entity entity, JSONArray records, int limit, int offset, String whereClause) {
        String countSql = whereClause != null
                ? String.format("select count(%s) from %s where %s",
                    entity.getPrimaryField().getName(), entity.getName(), whereClause)
                : String.format("select count(%s) from %s",
                    entity.getPrimaryField().getName(), entity.getName());

        int totalCount = records.size();
        try {
            Object[] countResult = Application.createQuery(countSql).unique();
            if (countResult != null && countResult.length > 0 && countResult[0] instanceof Number) {
                totalCount = ((Number) countResult[0]).intValue();
            }
        } catch (Exception ex) {
            log.warn("Failed to count total for {}", entity.getName(), ex);
        }

        boolean hasMore = offset + records.size() < totalCount;

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("entity", entity.getName());
        ret.put("entityLabel", EasyMetaFactory.getLabel(entity));
        Field nameField = entity.getNameField();
        if (nameField != null && !nameField.getName().equals(entity.getPrimaryField().getName())) {
            ret.put("nameField", nameField.getName());
            ret.put("nameFieldLabel", EasyMetaFactory.getLabel(nameField));
        }
        ret.put("total", totalCount);
        ret.put("hasMore", hasMore);
        ret.put("records", records);
        return ret;
    }

    /**
     * 将查询结果行构建为 JSON 对象
     *
     * @param entity
     * @param primaryField
     * @param nameField
     * @param queryFields
     * @param row
     * @return
     */
    private JSONObject buildRecordJson(Entity entity, Field primaryField, Field nameField,
                                      List<String> queryFields, Object[] row) {
        JSONObject record = new JSONObject();
        int idx = 0;

        // 主键（记录ID）
        record.put("id", wrapFieldValue(row[idx], primaryField));
        idx++;

        // 名称字段
        if (nameField != null && !nameField.getName().equals(primaryField.getName())) {
            record.put("name", wrapFieldValue(row[idx], nameField));
            idx++;
        }

        // 其他字段
        for (String fieldName : queryFields) {
            Field field = entity.getField(fieldName);
            record.put(fieldName, wrapFieldValue(row[idx], field));
            idx++;
        }

        return record;
    }

    private Object wrapFieldValue(Object value, Field field) {
        return FieldValueHelper.wrapFieldValue(value, field, true);
    }
}
