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
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
     * 解析 ID 参数（必填）并校验实体类型。调用方已知目标实体类型时使用，避免将其他实体的记录 ID 当作目标写入
     *
     * @param idStr
     * @param notNullParam
     * @param entityCode
     * @return
     */
    public static ID resolveId(String idStr, String notNullParam, int entityCode) {
        ID id = resolveId(idStr, notNullParam);
        if (id.getEntityCode() != entityCode) {
            throw new KnownToolException(notNullParam + " 不是 "
                    + EasyMetaFactory.getLabel(MetadataHelper.getEntity(entityCode)) + " 的记录 ID: " + idStr);
        }
        return id;
    }

    /**
     * 记录是否属于指定实体。注意 getEntityCode() 返回 Integer，必须数值比较；
     * 直接使用 != 是引用比较，超出 -128~127 缓存的实体编码（如用户自定义实体 990+）会误判
     *
     * @param recordId
     * @param entity
     * @return
     */
    public static boolean isSameEntity(ID recordId, Entity entity) {
        // Note: getEntityCode() 返回 Integer，必须数值比较 — 见 .agents/notes/implemented/bug-fix/2026-10-10-integer-boxing-entitycode-compare.md
        return (int) recordId.getEntityCode() == (int) entity.getEntityCode();
    }

    /**
     * 校验记录 ID 与目标实体匹配，不匹配时抛出异常（实体类型为运行时解析时使用，区别于按实体码校验的 resolveId）
     *
     * @param recordId
     * @param entity
     */
    public static void checkRecordEntity(ID recordId, Entity entity) {
        if (!isSameEntity(recordId, entity)) {
            throw new KnownToolException("记录 ID 与实体不匹配 : " + recordId
                    + " 不属于 " + EasyMetaFactory.getLabel(entity)
                    + "，实际属于 " + EasyMetaFactory.getLabel(MetadataHelper.getEntity(recordId.getEntityCode())));
        }
    }

    /**
     * 解析文件 key 参数（支持单个字符串或数组），并校验值域（rb/ 开头的系统内文件标识或 http(s) URL）
     *
     * @param value
     * @return
     */
    public static String resolveFileKeys(Object value) {
        if (value == null) return null;

        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            if (arr.isEmpty()) return null;
            for (Object o : arr) {
                checkFileKey(o == null ? null : o.toString().trim());
            }
            return arr.toJSONString();
        }

        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        checkFileKey(str);
        return JSON.toJSONString(new String[]{str});
    }

    /**
     * 校验文件 key 值域，非法值直接报错以便模型自我纠正
     *
     * @param fileKey
     */
    private static void checkFileKey(String fileKey) {
        if (StringUtils.isNotBlank(fileKey)
                && (fileKey.startsWith("rb/") || CommonsUtils.isExternalUrl(fileKey))) return;
        throw new KnownToolException("不支持的文件标识（仅支持 rb/ 开头或 http(s) URL）: " + fileKey);
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
            // 不校验实体码会把 Account 记录、Team、Department 等 ID 当用户写入下游
            ID id = ID.valueOf(userIdent);
            if (id.getEntityCode() != EntityHelper.User) return null;
            return Application.getUserStore().existsAny(id) ? id : null;
        }

        ID user = UserHelper.findUserByFullName(userIdent);
        if (user == null && Application.getUserStore().existsName(userIdent)) {
            user = Application.getUserStore().getUser(userIdent).getId();
        }
        return user;
    }

    /**
     * 解析记录 ID 列表参数（支持数组或字符串，字符串支持逗号分隔），去重
     *
     * @param value
     * @param max 一次最多处理的记录数
     * @return
     */
    public static List<ID> resolveRecordIds(Object value, int max) {
        Set<ID> result = new LinkedHashSet<>();

        for (String s : splitItems(value)) {
            if (StringUtils.isBlank(s)) continue;

            if (!ID.isId(s)) {
                throw new KnownToolException("无效的记录 ID : " + s + "，请使用 QueryRecords 工具查询获取");
            }
            result.add(ID.valueOf(s));
        }

        if (result.isEmpty()) {
            throw new KnownToolException("记录 ID (recordIds) 不能为空");
        }
        if (result.size() > max) {
            throw new KnownToolException("一次最多处理 " + max + " 条记录，当前 " + result.size() + " 条");
        }
        return new ArrayList<>(result);
    }

    /**
     * 解析用户列表参数（支持数组或字符串，字符串支持逗号分隔），去重。值为空时返回空列表
     *
     * @param value
     * @return
     */
    public static List<ID> resolveUsers(Object value) {
        Set<ID> result = new LinkedHashSet<>();

        for (String s : splitItems(value)) {
            if (StringUtils.isBlank(s)) continue;

            ID user = resolveUser(s);
            if (user == null) {
                throw new KnownToolException("未找到用户 : " + s + "，请填写用户全名或用户 ID");
            }
            result.add(user);
        }
        return new ArrayList<>(result);
    }

    /**
     * 解析级联实体参数（数组，元素为实体名称或标签），返回内部实体名数组
     *
     * @param cascades
     * @return
     */
    public static String[] resolveCascades(JSONArray cascades) {
        if (cascades == null || cascades.isEmpty()) return new String[0];

        Set<String> result = new LinkedHashSet<>();
        for (Object o : cascades) {
            if (o == null) continue;

            String s = o.toString().trim();
            if (StringUtils.isBlank(s)) continue;

            Entity entity = resolveEntity(s);
            if (entity == null) {
                throw new KnownToolException("相关实体不存在 : " + s + suggestEntity(s));
            }
            result.add(entity.getName());
        }
        return result.toArray(new String[0]);
    }

    /**
     * 参数值归一化为字符串列表（数组逐项，字符串按逗号分隔），均做 trim 处理
     *
     * @param value
     * @return
     */
    private static List<String> splitItems(Object value) {
        List<String> items = new ArrayList<>();
        if (value instanceof JSONArray) {
            for (Object o : (JSONArray) value) {
                if (o == null) continue;
                items.add(o.toString().trim());
            }
        } else if (value != null) {
            for (String s : value.toString().split(",")) {
                items.add(s.trim());
            }
        }
        return items;
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
     * 校验过滤条件。除了结构合法性，还会检查字段名是否真实存在
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
     * @param parser
     */
    private static void checkParseErrors(AdvFilterParser parser) {
        List<String> parseErrors;
        try {
            parseErrors = parser.getParseErrors();
        } catch (Exception ignored) {
            return;
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

    /**
     * 查询记录显示名列表（用于确认摘要），最多返回 max 条，按传入 ids 顺序；查不到时以记录 ID 兜底
     *
     * @param entity
     * @param ids
     * @param max
     * @return
     */
    public static List<String> recordNames(Entity entity, List<ID> ids, int max) {
        List<String> names = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return names;

        List<ID> use = ids.size() > max ? ids.subList(0, max) : ids;

        Field primaryField = entity.getPrimaryField();
        Field nameField = entity.getNameField();

        // 无名称字段时无法取名称，直接以记录 ID 代替
        if (nameField == null || nameField.getName().equals(primaryField.getName())) {
            for (ID id : use) names.add(id.toString());
            return names;
        }

        // 一次聚合查询取回名称，避免逐条查询
        String sql = String.format("select %s,%s from %s where %s in ('%s')",
                primaryField.getName(), nameField.getName(), entity.getName(),
                primaryField.getName(), StringUtils.join(use, "','"));

        Map<String, String> nameMap = new LinkedHashMap<>();
        for (Object[] o : Application.createQueryNoFilter(sql).array()) {
            if (o[0] == null) continue;

            Object nameValue = FieldValueHelper.wrapFieldValue(o[1], nameField, true);
            nameMap.put(o[0].toString(), nameValue == null ? null : String.valueOf(nameValue));
        }

        for (ID id : use) {
            String name = nameMap.get(id.toString());
            names.add(StringUtils.isBlank(name) ? id.toString() : name);
        }
        return names;
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
     * 尝试将文本解析为 JSON，失败时原样返回字符串；空文本返回 null
     *
     * @param text
     * @return
     */
    public static Object parseJsonOrRaw(String text) {
        if (StringUtils.isBlank(text)) return null;
        try {
            return JSONUtils.parseSafe(text);
        } catch (Exception ex) {
            return text;
        }
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
