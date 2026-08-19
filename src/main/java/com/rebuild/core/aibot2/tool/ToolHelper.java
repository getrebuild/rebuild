/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 工具通用帮助类
 *
 * @author devezhao
 * @since 2026/7/24
 */
public class ToolHelper {

    private ToolHelper() {}

    // ----------------------------------------------------------------
    //  参数解析
    // ----------------------------------------------------------------

    /**
     * 解析 ID 参数（可选）。字符串为空或非合法 ID 时返回 null
     *
     * @param idStr
     * @return
     */
    public static ID resolveId(String idStr) {
        return ID.isId(idStr) ? ID.valueOf(idStr) : null;
    }

    /**
     * 解析 ID 参数（必填）。字符串为空或非合法 ID 时抛出异常
     *
     * @param idStr
     * @param notNullParam
     * @return
     */
    public static ID resolveId(String idStr, String notNullParam) {
        if (ID.isId(idStr)) return ID.valueOf(idStr);
        throw new KnownToolException(notNullParam + " 不是有效的 ID: " + idStr);
    }

    /**
     * 解析文件 key 参数（支持单个字符串或数组）
     *
     * @param value
     * @return
     */
    public static String resolveFileKeys(Object value) {
        if (value == null) return null;

        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            return arr.isEmpty() ? null : arr.toJSONString();
        }

        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        return JSON.toJSONString(new String[]{str});
    }

    /**
     * 解析实体（支持名称、code、标签匹配）
     * 精确匹配优先，多个匹配时抛出异常要求询问用户选择（禁止自行决定）
     *
     * @param name
     * @return
     */
    public static Entity resolveEntity(String name) {
        if (StringUtils.isBlank(name)) return null;

        // 1. 精确匹配实体名称（名称唯一，直接返回；同名歧义仅在标签匹配分支处理）
        if (MetadataHelper.containsEntity(name)) {
            return MetadataHelper.getEntity(name);
        }

        if (StringUtils.isNumeric(name)) {
            int code = Integer.parseInt(name);
            if (MetadataHelper.containsEntity(code)) {
                return MetadataHelper.getEntity(code);
            }
        }

        String nameLower = name.toLowerCase();
        List<Entity> exactMatches = new ArrayList<>();
        List<Entity> fuzzyMatches = new ArrayList<>();

        for (Entity e : MetadataHelper.getEntities()) {
            String label = EasyMetaFactory.getLabel(e);
            if (StringUtils.isBlank(label)) continue;

            // 精确标签匹配（可能有同名实体，不能直接返回）
            if (label.equalsIgnoreCase(name)) {
                exactMatches.add(e);
            } else if (label.toLowerCase().contains(nameLower)) {
                fuzzyMatches.add(e);
            }
        }

        if (exactMatches.size() == 1) return exactMatches.get(0);
        if (!exactMatches.isEmpty()) return throwAmbiguousEntities(name, exactMatches);

        if (fuzzyMatches.isEmpty()) return null;
        if (fuzzyMatches.size() == 1) return fuzzyMatches.get(0);

        return throwAmbiguousEntities(name, fuzzyMatches);
    }

    /**
     * 多个实体匹配时抛出异常，明确要求模型询问用户而非自行选择
     *
     * @param name
     * @param matches
     * @return
     */
    private static Entity throwAmbiguousEntities(String name, List<Entity> matches) {
        JSONArray list = new JSONArray();
        for (Entity e : matches) {
            list.add(JSONUtils.toJSONObject(
                    new String[]{"name", "label"},
                    new Object[]{e.getName(), EasyMetaFactory.getLabel(e)}));
        }
        throw new KnownToolException("「" + name + "」匹配到多个实体 : " + list.toJSONString()
                + "。请将候选列表转述给用户并询问具体是哪一个，由用户选择后再继续，禁止自行决定");
    }

    /**
     * 解析字段（支持字段名、标签）。未匹配时抛出异常并附候选提示
     *
     * @param entity
     * @param fieldIdent
     * @return
     */
    public static Field resolveField(Entity entity, String fieldIdent) {
        if (StringUtils.isBlank(fieldIdent)) {
            throw new KnownToolException("字段名不能为空");
        }

        if (entity.containsField(fieldIdent)) {
            return entity.getField(fieldIdent);
        }

        for (Field f : entity.getFields()) {
            if (MetadataHelper.isSystemField(f)) continue;
            if (fieldIdent.equalsIgnoreCase(EasyMetaFactory.getLabel(f))) {
                return f;
            }
        }

        throw new KnownToolException(String.format("字段不存在 : %s.%s %s",
                entity.getName(), fieldIdent, suggestField(entity, fieldIdent)));
    }

    /**
     * 解析字段路径（支持「字段.子字段」跨引用实体，名称或标签均可）
     *
     * @param entity
     * @param path
     * @return
     */
    public static String resolveFieldPath(Entity entity, String path) {
        if (!path.contains(".")) {
            return resolveField(entity, path).getName();
        }

        String[] segs = path.split("\\.");
        Entity current = entity;
        StringBuilder resolved = new StringBuilder();
        for (int i = 0; i < segs.length; i++) {
            Field f = resolveField(current, segs[i]);
            if (resolved.length() > 0) resolved.append(".");
            resolved.append(f.getName());

            if (i < segs.length - 1) {
                if (f.getType() != FieldType.REFERENCE || f.getReferenceEntity() == null) {
                    throw new KnownToolException("字段路径中 " + segs[i] + " 不是引用字段，无法继续向下引用");
                }
                current = f.getReferenceEntity();
            }
        }
        return resolved.toString();
    }

    /**
     * 解析用户（支持 ID、全名、用户名）
     *
     * @param userIdent
     * @return
     */
    public static ID resolveUser(String userIdent) {
        if (StringUtils.isBlank(userIdent)) return null;

        if (ID.isId(userIdent)) {
            return ID.valueOf(userIdent);
        }

        ID user = UserHelper.findUserByFullName(userIdent);
        if (user == null && Application.getUserStore().existsName(userIdent)) {
            user = Application.getUserStore().getUser(userIdent).getId();
        }
        return user;
    }

    // ----------------------------------------------------------------
    //  字段/实体提示
    // ----------------------------------------------------------------

    /**
     * 模糊匹配相似字段名
     *
     * @param entity
     * @param fieldName
     * @return
     */
    public static String suggestField(Entity entity, String fieldName) {
        if (StringUtils.isBlank(fieldName)) return "";

        String lower = fieldName.toLowerCase();
        List<String> candidates = new ArrayList<>();

        for (Field f : entity.getFields()) {
            if (MetadataHelper.isSystemField(f)) continue;
            String fn = f.getName().toLowerCase();
            if (fn.contains(lower) || lower.contains(fn)) {
                candidates.add(f.getName());
            }
        }

        if (candidates.isEmpty()) return "";
        return candidates.size() == 1
                ? "，你是否想用 " + candidates.get(0) + "？"
                : "，相似字段: " + StringUtils.join(candidates, ", ");
    }

    /**
     * 模糊匹配相似实体名
     *
     * @param entityName
     * @return
     */
    public static String suggestEntity(String entityName) {
        if (StringUtils.isBlank(entityName)) return "";

        String lower = entityName.toLowerCase();
        List<String> candidates = new ArrayList<>();

        for (Entity e : MetadataHelper.getEntities()) {
            if (!MetadataHelper.isBusinessEntity(e)) continue;

            String eName = e.getName().toLowerCase();
            String eLabel = EasyMetaFactory.getLabel(e);
            String eLabelLower = eLabel != null ? eLabel.toLowerCase() : "";

            if (eName.contains(lower) || lower.contains(eName)
                    || eLabelLower.contains(lower) || lower.contains(eLabelLower)) {
                candidates.add(eLabel + "(" + e.getName() + ")");
            }
        }

        if (candidates.isEmpty()) return "";
        return candidates.size() == 1
                ? "，你是否想用 " + candidates.get(0) + "？"
                : "，相似实体: " + StringUtils.join(candidates, ", ");
    }

    /**
     * 列出实体中可用的字段名
     *
     * @param entity
     * @return
     */
    public static String listFields(Entity entity) {
        List<String> fields = new ArrayList<>();
        for (Field f : entity.getFields()) {
            if (MetadataHelper.isSystemField(f)) continue;
            fields.add(f.getName());
        }
        return fields.isEmpty() ? "（无）" : StringUtils.join(fields, ", ");
    }

    // ----------------------------------------------------------------
    //  过滤条件
    // ----------------------------------------------------------------

    /**
     * 构建过滤表达式 AdvFilterParser
     *
     * @param entity
     * @param filter
     * @param equation
     * @return
     */
    public static JSONObject buildFilterExpr(Entity entity, JSONArray filter, String equation) {
        JSONObject filterExpr = new JSONObject();
        filterExpr.put("entity", entity.getName());
        filterExpr.put("items", filter != null ? filter : new JSONArray());
        if (StringUtils.isNotBlank(equation)) {
            filterExpr.put("equation", equation);
        }
        return filterExpr;
    }

    /**
     * 校验过滤条件。除了结构合法性，还会检查字段名是否真实存在——
     * AdvFilterParser 对无效字段/值等静默跳过并记录到 parseErrors，
     * 此方法通过 getParseErrors() 检测被忽略的无效配置项。
     *
     * @param entity
     * @param filterExpr
     */
    public static void validateFilter(Entity entity, JSONObject filterExpr) {
        AdvFilterParser parser;
        try {
            parser = new AdvFilterParser(filterExpr, entity);
            parser.toSqlWhere();
        } catch (Exception ex) {
            throw new KnownToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }
        checkParseErrors(parser);
    }

    /**
     * 检查过滤条件中是否有被 AdvFilterParser 静默忽略的无效配置项。
     * AdvFilterParser.parseItem 对无效字段/值等调用 parseError 记录并返回 null（不抛异常），
     * 导致无效配置项被静默跳过、查询范围比预期更宽。此方法通过 getParseErrors()
     * 检测被忽略的无效配置项并抛出异常。
     *
     * @param parser 已执行过 toSqlWhere 的 AdvFilterParser
     */
    private static void checkParseErrors(AdvFilterParser parser) {
        List<String> parseErrors;
        try {
            parseErrors = parser.getParseErrors();
        } catch (Exception ignored) {
            return;  // toSqlWhere 提前返回（如 filterExpr 为空），无需校验
        }
        if (!parseErrors.isEmpty()) {
            throw new KnownToolException("过滤条件中存在无效配置项，请确认使用了 ListEntities 返回的真实字段名。"
                    + "无效项: " + joinErrors(parseErrors));
        }
    }

    /**
     * 解析过滤条件为 SQL 子句
     *
     * @param entity
     * @param filter
     * @param equation
     * @return
     */
    public static String parseFilterToWhere(Entity entity, JSONArray filter, String equation) {
        if (filter == null || filter.isEmpty()) return null;
        JSONObject filterExpr = buildFilterExpr(entity, filter, equation);
        AdvFilterParser parser = new AdvFilterParser(filterExpr, entity);
        String whereClause;
        try {
            whereClause = parser.toSqlWhere();
        } catch (Exception ex) {
            throw new KnownToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }
        checkParseErrors(parser);
        return whereClause;
    }

    // ----------------------------------------------------------------
    //  查询结果构建
    // ----------------------------------------------------------------

    /**
     * 构建查询字段列表（不含主键和名称字段，它们会被单独添加）
     *
     * @param entity
     * @param fields
     * @param invalidFields
     * @return
     */
    public static List<String> buildQueryFields(Entity entity, String fields, JSONArray invalidFields) {
        Set<String> result = new LinkedHashSet<>();
        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        if (StringUtils.isBlank(fields)) {
            for (Field f : entity.getFields()) {
                if (MetadataHelper.isSystemField(f)) continue;
                if (f.getType() == FieldType.PRIMARY) continue;
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
                // 使用 resolveField 支持字段名和中文标签，返回真实字段名
                try {
                    Field field = resolveField(entity, f);
                    String fn = field.getName();
                    if (fn.equals(primaryField.getName())) continue;
                    if (nameField != null && fn.equals(nameField.getName())) continue;
                    result.add(fn);
                } catch (KnownToolException ex) {
                    if (invalidFields != null) {
                        JSONObject invalid = new JSONObject();
                        invalid.put("name", f);
                        String suggestion = ToolHelper.suggestField(entity, f);
                        if (StringUtils.isNotBlank(suggestion)) invalid.put("suggestion", suggestion);
                        invalidFields.add(invalid);
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 构建 SQL 字段列表
     *
     * @param primaryField
     * @param nameField
     * @param queryFields
     * @return
     */
    public static String buildFieldsSql(Field primaryField, Field nameField, List<String> queryFields) {
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
    public static JSONObject buildRecordJson(Entity entity, Field primaryField, Field nameField, List<String> queryFields, Object[] row) {
        JSONObject record = new JSONObject();
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

    /**
     * 包装字段值为可读格式
     *
     * @param value
     * @param field
     * @return
     */
    public static Object wrapFieldValue(Object value, Field field) {
        return FieldValueHelper.wrapFieldValue(value, field, true);
    }

    // ----------------------------------------------------------------
    //  通用工具
    // ----------------------------------------------------------------

    /**
     * 拼接错误明细（限制条数避免过长）
     *
     * @param errors
     * @return
     */
    public static String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) return "";
        List<String> use = errors.size() > 5 ? errors.subList(0, 5) : errors;
        String s = StringUtils.join(use, "；");
        return errors.size() > 5 ? s + "（等共 " + errors.size() + " 条错误）" : s;
    }

    /**
     * JSON 压缩为单行紧凑格式
     *
     * @param text
     * @return
     */
    public static String compactJson(String text) {
        if (JSONUtils.wellFormat(text)) {
            try {
                return JSON.toJSONString(JSON.parse(text));
            } catch (Exception ignored) {
            }
        }
        return text;
    }
}
