/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RecordDataCleaner;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.DeleteRecord;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 新建/更新业务记录，字段数据由模型直接组装（纯执行器，不做 AI 解析）
 *
 * @author RB
 * @since 2026/9/23
 */
@Slf4j
public class UpsertRecord implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityName = args.getString("entity");
        String recordId = args.getString("recordId");
        boolean confirmed = args.getBooleanValue("confirmed");
        JSONObject recordData = args.getJSONObject("recordData");

        if (StringUtils.isBlank(entityName)) {
            throw new KnownToolException("实体名称不能为空");
        }
        if (recordData == null || recordData.isEmpty()) {
            throw new KnownToolException("记录数据 recordData 不能为空");
        }

        Entity entity = ToolHelper.resolveEntity(entityName);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityName + ToolHelper.suggestEntity(entityName));
        }
        if (!entity.isQueryable() || !MetadataHelper.isBusinessEntity(entity)) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不支持此操作");
        }

        JSONObject recordJson = toFormJson(entity, recordData);
        validateRecordData(entity, recordJson);

        if (!confirmed) {
            JSONObject changes = new JSONObject(true);
            changes.put("操作", StringUtils.isNotBlank(recordId) && ID.isId(recordId) ? "更新记录" : "新建记录");
            changes.put("目标实体", EasyMetaFactory.getLabel(entity));
            changes.put("记录数据", recordData);
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。保存记录会影响业务数据，请先将记录数据摘要（关键字段值与明细条数）完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行保存。"
                                    + "用户未确认或要求调整时不得执行保存"});
        }

        return saveRecord(recordJson, entity, recordId);
    }

    /**
     * 模型契约（明细键 $DETAILS$，元素内 entity/id/delete 平铺）转为表单兼容结构（metadata 包裹），不修改传入的 recordData
     *
     * @param entity
     * @param recordData
     * @return
     */
    private JSONObject toFormJson(Entity entity, JSONObject recordData) {
        JSONObject recordJson = new JSONObject(true);
        recordJson.put("metadata", JSONUtils.toJSONObject("entity", entity.getName()));

        JSONObject copy = new JSONObject(true);
        copy.putAll(recordData);
        Object details = copy.remove(GeneralEntityService.HAS_DETAILS);
        // 模型误用 details 键时给出明确指引（实体本身有 details 业务字段时不拦截）
        if (copy.containsKey("details") && findField(entity, "details") == null) {
            throw new KnownToolException("明细数据须放入 $DETAILS$ 数组，而不是 details");
        }
        recordJson.putAll(copy);

        if (details instanceof JSONArray) {
            JSONArray formDetails = new JSONArray();
            for (Object d : (JSONArray) details) {
                if (!(d instanceof JSONObject)) {
                    throw new KnownToolException("$DETAILS$ 中的明细元素必须是 JSON 对象");
                }
                formDetails.add(toFormDetail((JSONObject) d));
            }
            if (!formDetails.isEmpty()) {
                recordJson.put(GeneralEntityService.HAS_DETAILS, formDetails);
            }
        } else if (details != null) {
            throw new KnownToolException("$DETAILS$ 必须是明细数组");
        }
        return recordJson;
    }

    private JSONObject toFormDetail(JSONObject detail) {
        JSONObject formDetail = new JSONObject(true);
        JSONObject metadata = new JSONObject(true);

        String detailEntity = detail.getString("entity");
        if (StringUtils.isNotBlank(detailEntity)) metadata.put("entity", detailEntity);

        String detailId = detail.getString("id");
        if (StringUtils.isNotBlank(detailId)) metadata.put("id", detailId);

        if (detail.getBooleanValue("delete")) metadata.put("delete", true);

        formDetail.put("metadata", metadata);

        for (String key : detail.keySet()) {
            if ("entity".equalsIgnoreCase(key) || "id".equalsIgnoreCase(key) || "delete".equalsIgnoreCase(key)) continue;
            formDetail.put(key, detail.get(key));
        }
        return formDetail;
    }

    /**
     * 预检：字段名归一化与存在性校验、引用 ID 有效性校验（清洗层对未知字段与无效引用会静默丢弃，须在保存前拦截）
     *
     * @param entity
     * @param recordJson
     */
    private void validateRecordData(Entity entity, JSONObject recordJson) {
        normalizeFields(entity, recordJson);

        JSONArray details = recordJson.getJSONArray(GeneralEntityService.HAS_DETAILS);
        if (details == null) return;

        for (Object d : details) {
            JSONObject detail = (JSONObject) d;
            JSONObject metadata = detail.getJSONObject("metadata");

            if (metadata != null && metadata.getBooleanValue("delete")) {
                validateDetailDelete(entity, metadata.getString("id"));
                continue;
            }

            Entity detailEntity = getDetailEntity(entity, metadata == null ? null : metadata.getString("entity"));
            if (detailEntity == null) continue;

            if (metadata == null) {
                metadata = new JSONObject();
                detail.put("metadata", metadata);
            }
            metadata.put("entity", detailEntity.getName());

            normalizeFields(detailEntity, detail);
        }
    }

    /**
     * 字段名按内部名归一化（忽略大小写差异），未知字段与无效引用值直接报错
     *
     * @param entity
     * @param data
     */
    private void normalizeFields(Entity entity, JSONObject data) {
        List<String> unknownFields = new ArrayList<>();
        List<String> invalidRefs = new ArrayList<>();

        for (String key : data.keySet().toArray(new String[0])) {
            if ("metadata".equals(key) || GeneralEntityService.HAS_DETAILS.equals(key)) continue;

            Field field = findField(entity, key);
            if (field == null) {
                unknownFields.add(key);
                continue;
            }

            if (!field.getName().equals(key)) {
                data.put(field.getName(), data.remove(key));
            }

            Object value = data.get(field.getName());
            if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) continue;

            DisplayType dt = EasyMetaFactory.getDisplayType(field);

            if (dt == DisplayType.REFERENCE) {
                if (!(value instanceof String) || !isValidRefId(field, (String) value)) {
                    invalidRefs.add(EasyMetaFactory.getLabel(field) + "=" + CommonsUtils.maxstr(String.valueOf(value), 100));
                }
            } else if (dt == DisplayType.N2NREFERENCE) {
                if (!(value instanceof JSONArray)) {
                    invalidRefs.add(EasyMetaFactory.getLabel(field) + "（值须为记录 ID 数组）");
                } else {
                    for (Object v : (JSONArray) value) {
                        if (!(v instanceof String) || !isValidRefId(field, (String) v)) {
                            invalidRefs.add(EasyMetaFactory.getLabel(field) + "=" + CommonsUtils.maxstr(String.valueOf(v), 100));
                            break;
                        }
                    }
                }
            }
        }

        if (!unknownFields.isEmpty()) {
            throw new KnownToolException("字段 " + StringUtils.join(unknownFields, ", ")
                    + " 不存在于实体 [" + EasyMetaFactory.getLabel(entity) + "]，可用字段 : " + listFieldNames(entity));
        }
        if (!invalidRefs.isEmpty()) {
            throw new KnownToolException("引用字段 " + StringUtils.join(invalidRefs, "; ")
                    + " 的值无效，必须为已存在的记录 ID。请使用 QueryRecords 工具查询引用记录获取 ID，"
                    + "记录不存在时须经用户确认后先创建该记录再填入其 ID，不得使用名称或其他文本");
        }
    }

    private boolean isValidRefId(Field field, String value) {
        if (!ID.isId(value)) return false;
        ID id = ID.valueOf(value);
        ToolHelper.checkRecordEntity(id, field.getReferenceEntity());
        return QueryHelper.exists(id);
    }

    private void validateDetailDelete(Entity mainEntity, String detailId) {
        if (StringUtils.isBlank(detailId) || !ID.isId(detailId)) {
            throw new KnownToolException("删除明细须提供有效的明细记录 ID");
        }
        ID id = ID.valueOf(detailId);
        Entity detailEntity = MetadataHelper.getEntity(id.getEntityCode());
        if (detailEntity == null || detailEntity.getMainEntity() == null
                || !detailEntity.getMainEntity().getName().equals(mainEntity.getName())) {
            throw new KnownToolException("记录 " + detailId + " 不是 "
                    + EasyMetaFactory.getLabel(mainEntity) + " 的明细，无法删除");
        }
        if (!QueryHelper.exists(id)) {
            throw new KnownToolException("待删除的明细 " + detailId + " 不存在或已被删除");
        }
    }

    private Field findField(Entity entity, String name) {
        for (Field f : entity.getFields()) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    private String listFieldNames(Entity entity) {
        List<String> names = new ArrayList<>();
        for (Field f : entity.getFields()) {
            if (MetadataHelper.isSystemField(f)) continue;
            names.add(f.getName() + "(" + EasyMetaFactory.getLabel(f) + ")");
        }
        return CommonsUtils.maxstr(StringUtils.join(names, ", "), 1000);
    }

    private JSONObject saveRecord(JSONObject recordJson, Entity entity, String recordId) {
        ID userId = UserContextHolder.getUser();

        boolean isUpdate = StringUtils.isNotBlank(recordId) && ID.isId(recordId);
        if (isUpdate) {
            ToolHelper.checkRecordEntity(ID.valueOf(recordId), entity);
        }
        if (!isUpdate && !entity.isCreatable()) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不允许新建记录");
        }
        if (isUpdate && !entity.isUpdatable()) {
            throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(entity) + "] 不允许更新记录");
        }

        // 新建时移除模型可能臆造的记录 ID，避免误走更新分支（更新仅由 recordId 参数决定）
        if (!isUpdate) {
            JSONObject metadata = recordJson.getJSONObject("metadata");
            if (metadata != null) metadata.remove("id");
        }

        Object detailsObj = recordJson.remove(GeneralEntityService.HAS_DETAILS);
        JSONArray detailsJson = detailsObj instanceof JSONArray ? (JSONArray) detailsObj : null;

        JSONObject cleanedMain = RecordDataCleaner.cleanPostData(entity, recordJson);

        if (StringUtils.isNotBlank(recordId) && ID.isId(recordId)) {
            JSONObject metadata = cleanedMain.getJSONObject("metadata");
            if (metadata == null) {
                metadata = new JSONObject();
                cleanedMain.put("metadata", metadata);
            }
            metadata.put("id", recordId);
        }

        Record record = EntityHelper.parse(cleanedMain, userId);

        if (detailsJson != null && !detailsJson.isEmpty()) {
            List<Record> detailsList = new ArrayList<>();
            for (Object d : detailsJson) {
                JSONObject detailJson = (JSONObject) d;
                JSONObject detailMeta = detailJson.getJSONObject("metadata");

                // 删除已有明细（与 Web 表单 metadata.delete 用法一致，仅更新已有主记录时有效）
                if (detailMeta != null && detailMeta.getBooleanValue("delete")) {
                    if (!isUpdate) {
                        throw new KnownToolException("新建记录不支持删除明细");
                    }
                    ID deleteId = ToolHelper.resolveId(detailMeta.getString("id"), "删除明细 ID");
                    Entity deleteEntity = MetadataHelper.getEntity(deleteId.getEntityCode());
                    if (deleteEntity == null || deleteEntity.getMainEntity() == null
                            || !deleteEntity.getMainEntity().getName().equals(entity.getName())) {
                        throw new KnownToolException("记录 " + deleteId + " 不是 "
                                + EasyMetaFactory.getLabel(entity) + " 的明细，无法删除");
                    }
                    detailsList.add(new DeleteRecord(deleteId, userId));
                    continue;
                }

                if (detailMeta == null || StringUtils.isBlank(detailMeta.getString("entity"))) {
                    Entity detailEntity = getDetailEntity(entity,
                            detailMeta == null ? null : detailMeta.getString("entity"));
                    if (detailEntity == null) {
                        log.warn("无法匹配明细实体，跳过此明细 : {}", detailJson);
                        continue;
                    }
                    if (detailMeta == null) {
                        detailMeta = new JSONObject();
                        detailJson.put("metadata", detailMeta);
                    }
                    detailMeta.put("entity", detailEntity.getName());
                }

                // 模型可能显式传入任意实体名，须校验其确为本主实体的明细
                String detailEntityName = detailJson.getJSONObject("metadata").getString("entity");
                Entity detailEntity = MetadataHelper.getEntity(detailEntityName);
                if (detailEntity.getMainEntity() == null
                        || !detailEntity.getMainEntity().getName().equals(entity.getName())) {
                    throw new KnownToolException("明细实体 " + detailEntityName + " 不属于 "
                            + EasyMetaFactory.getLabel(entity) + "，可用明细实体: " + listDetailEntityNames(entity));
                }
                JSONObject cleanedDetail = RecordDataCleaner.cleanPostData(detailEntity, detailJson);

                // DTF（明细关联主记录）字段由框架保存时自动填充，新建明细预置占位值
                // 以通过 EntityHelper.parse 的必填校验（与 Web 表单行为一致）
                String detailId = cleanedDetail.getJSONObject("metadata") == null
                        ? null : cleanedDetail.getJSONObject("metadata").getString("id");
                String dtfName = MetadataHelper.getDetailToMainField(detailEntity).getName();
                if (StringUtils.isBlank(detailId) && StringUtils.isBlank(cleanedDetail.getString(dtfName))) {
                    cleanedDetail.put(dtfName, isUpdate ? recordId : EntityHelper.UNSAVED_ID.toString());
                }
                detailsList.add(EntityHelper.parse(cleanedDetail, userId));
            }
            if (!detailsList.isEmpty()) {
                record.setObjectValue(GeneralEntityService.HAS_DETAILS, detailsList);
            }
        }

        GeneralEntityServiceContextHolder.setRepeatedCheckMode(GeneralEntityServiceContextHolder.RCM_CHECK_ALL);
        if (record.getPrimary() == null) {
            GeneralEntityServiceContextHolder.setSkipSeriesValue();
        }

        EntityService es = Application.getEntityService(record.getEntity().getEntityCode());
        boolean isNew = record.getPrimary() == null;
        try {
            record = es.createOrUpdate(record);
        } catch (Exception ex) {
            throw new KnownToolException("保存记录失败 : " + CommonsUtils.getRootMessage(ex), ex);
        } finally {
            GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
            GeneralEntityServiceContextHolder.isSkipSeriesValue(true);
        }

        String url = AppUtils.getContextPath("/app/redirect?id=" + record.getPrimary() + "&type=newtab");
        String message = String.format("已%s记录，[点击查看记录](%s)，请将此链接展示给用户核对",
                isNew ? "成功创建" : "成功更新", url);
        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "action", "url", "message"},
                new Object[]{"ok", record.getPrimary(), isNew ? "created" : "updated", url, message});
    }

    private Entity getDetailEntity(Entity mainEntity, String hint) {
        Entity[] details = MetadataSorter.sortDetailEntities(mainEntity);
        if (details.length == 0) return null;
        if (details.length == 1) return details[0];

        if (StringUtils.isNotBlank(hint)) {
            for (Entity de : details) {
                if (de.getName().equalsIgnoreCase(hint) || EasyMetaFactory.getLabel(de).equalsIgnoreCase(hint)) {
                    return de;
                }
            }
        }
        // 多个明细且无法确定归属时不能默认取第一个，否则明细数据会静默写入错误的明细实体
        throw new KnownToolException("实体 [" + EasyMetaFactory.getLabel(mainEntity) + "] 有多个明细实体，"
                + "请在明细的 entity 中明确指定 : " + listDetailEntityNames(mainEntity));
    }

    /**
     * 列出主实体的全部明细实体（名称与标签）
     *
     * @param mainEntity
     * @return
     */
    private String listDetailEntityNames(Entity mainEntity) {
        List<String> names = new ArrayList<>();
        for (Entity de : MetadataSorter.sortDetailEntities(mainEntity)) {
            names.add(de.getName() + "(" + EasyMetaFactory.getLabel(de) + ")");
        }
        return names.isEmpty() ? "无" : StringUtils.join(names, ", ");
    }
}
