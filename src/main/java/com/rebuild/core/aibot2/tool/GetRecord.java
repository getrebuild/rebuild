/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.FieldValueHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 获取单条记录的完整字段详情
 *
 * @author devezhao
 * @since 2026/8/9
 */
@Slf4j
public class GetRecord implements Tool {

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

        String recordId = args.getString("record");
        if (StringUtils.isBlank(recordId) || !ID.isId(recordId)) {
            throw new ToolException("记录ID (record) 不能为空且需为有效ID");
        }

        ID record = ID.valueOf(recordId);
        // 校验实体匹配
        if (record.getEntityCode() != entity.getEntityCode()) {
            throw new ToolException("记录ID与实体不匹配，记录ID对应的实体为 : "
                    + EasyMetaFactory.getLabel(MetadataHelper.getEntity(record.getEntityCode())));
        }

        String fields = args.getString("fields");
        JSONArray invalidFields = new JSONArray();
        List<String> queryFields = buildQueryFields(entity, fields, invalidFields);

        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        String fieldsSql = buildFieldsSql(primaryField, nameField, queryFields);
        String sql = String.format("select %s from %s where %s = '%s'",
                fieldsSql, entity.getName(), primaryField.getName(), record.toLiteral());

        Object[] row = Application.createQuery(sql).unique();
        if (row == null || row.length == 0) {
            throw new ToolException("未找到记录或无权限访问 : " + recordId);
        }

        JSONObject recordJson = buildRecordJson(entity, primaryField, nameField, queryFields, row);

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("entity", entity.getName());
        result.put("entityLabel", EasyMetaFactory.getLabel(entity));
        if (nameField != null && !nameField.getName().equals(primaryField.getName())) {
            result.put("nameField", nameField.getName());
            result.put("nameFieldLabel", EasyMetaFactory.getLabel(nameField));
        }
        result.put("record", recordJson);
        if (!invalidFields.isEmpty()) {
            result.put("invalidFields", invalidFields);
        }
        return result;
    }

    /**
     * 构建查询字段列表（不含主键和名称字段，它们会被单独添加）
     */
    private List<String> buildQueryFields(Entity entity, String fields, JSONArray invalidFields) {
        Set<String> result = new LinkedHashSet<>();
        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        if (StringUtils.isBlank(fields)) {
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
     */
    private JSONObject buildRecordJson(Entity entity, Field primaryField, Field nameField,
                                      List<String> queryFields, Object[] row) {
        JSONObject record = new JSONObject(true);
        int idx = 0;

        record.put("id", wrapFieldValue(row[idx], primaryField));
        idx++;

        if (nameField != null && !nameField.getName().equals(primaryField.getName())) {
            record.put("name", wrapFieldValue(row[idx], nameField));
            idx++;
        }

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
