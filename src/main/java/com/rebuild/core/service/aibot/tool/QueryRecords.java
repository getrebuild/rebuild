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
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
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
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName);
        }

        String name = args.getString("name");
        JSONArray filter = args.getJSONArray("filter");
        String fields = args.getString("fields");
        String sort = args.getString("sort");
        int limit = args.getIntValue("limit");
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        int pageNo = args.getIntValue("pageNo");
        if (pageNo < 1) pageNo = 1;

        // 构建查询字段列表
        List<String> queryFields = buildQueryFields(entity, fields);
        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        // 构建排序子句
        String orderBy = buildOrderBy(entity, sort);
        // 分页偏移量
        int offset = (pageNo - 1) * limit;

        // 按名称/编号模糊匹配
        if (StringUtils.isNotBlank(name)) {
            return queryByName(entity, primaryField, nameField, queryFields, name, limit, offset, orderBy);
        }

        // 按字段条件过滤
        if (filter != null && !filter.isEmpty()) {
            return queryByFilter(entity, primaryField, nameField, queryFields, filter, limit, offset, orderBy);
        }

        // 返回记录列表
        return queryList(entity, primaryField, nameField, queryFields, limit, offset, orderBy);
    }

    /**
     * 按名称/编号模糊匹配（搜索名称字段和 SERIES 字段）
     */
    private JSONObject queryByName(Entity entity, Field primaryField, Field nameField,
                                   List<String> queryFields, String name, int limit, int offset, String orderBy) {
        // 可搜索字段：名称字段 + SERIES 字段
        List<Field> searchFields = new ArrayList<>();
        if (nameField != null) {
            searchFields.add(nameField);
        }
        for (Field f : MetadataSorter.sortFields(entity, DisplayType.SERIES)) {
            if (!searchFields.contains(f)) searchFields.add(f);
        }

        if (searchFields.isEmpty()) {
            throw new ToolException("该实体没有可搜索的名称或编号字段");
        }

        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String likeValue = "'%" + name.replace("'", "''") + "%'";
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < searchFields.size(); i++) {
            if (i > 0) whereClause.append(" or ");
            whereClause.append(searchFields.get(i).getName()).append(" like ").append(likeValue);
        }

        String sql = String.format("select %s from %s where %s%s",
                fieldsSql, entity.getName(), whereClause, orderBy);

        Object[][] results = Application.createQueryNoFilter(sql).setLimit(limit, offset).array();
        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "total", "records"},
                new Object[]{"ok", entity.getName(), records.size(), records});
    }

    /**
     * 按字段条件过滤查询（使用 AdvFilterParser 解析条件）
     */
    private JSONObject queryByFilter(Entity entity, Field primaryField, Field nameField,
                                     List<String> queryFields, JSONArray filter, int limit, int offset, String orderBy) {
        // 构建 AdvFilterParser 所需的过滤表达式
        JSONObject filterExpr = new JSONObject();
        filterExpr.put("entity", entity.getName());
        filterExpr.put("items", filter);

        String whereClause;
        try {
            whereClause = new AdvFilterParser(filterExpr, entity).toSqlWhere();
        } catch (Exception ex) {
            throw new ToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }

        if (StringUtils.isBlank(whereClause)) {
            throw new ToolException("过滤条件无效，请检查字段名和操作符是否正确");
        }

        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s where %s%s",
                fieldsSql, entity.getName(), whereClause, orderBy);

        Object[][] results = Application.createQueryNoFilter(sql).setLimit(limit, offset).array();
        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "total", "records"},
                new Object[]{"ok", entity.getName(), records.size(), records});
    }

    /**
     * 返回记录列表
     */
    private JSONObject queryList(Entity entity, Field primaryField, Field nameField,
                                List<String> queryFields, int limit, int offset, String orderBy) {
        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s%s", fieldsSql, entity.getName(), orderBy);

        Object[][] results = Application.createQueryNoFilter(sql)
                .setLimit(limit, offset)
                .array();

        JSONArray records = new JSONArray();
        for (Object[] row : results) {
            records.add(buildRecordJson(entity, primaryField, nameField, queryFields, row));
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "total", "records"},
                new Object[]{"ok", entity.getName(), records.size(), records});
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
            throw new ToolException("排序字段不存在 : " + sortField);
        }

        return " order by " + sortField + " " + direction;
    }

    /**
     * 构建查询字段列表（不含主键和名称字段，它们会被单独添加）
     */
    private List<String> buildQueryFields(Entity entity, String fields) {
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
                if (!entity.containsField(f)) continue;
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
