/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RecordDataCleaner;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.aibot.vector.FileData;
import com.rebuild.core.service.aibot2.ChatManager;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将文件或文本内容解析为实体记录 JSON，默认保存，可选输出 JSON
 *
 * @author RB
 * @since 2026/6/11
 */
@Slf4j
public class UpsertRecord implements Tool {

    private static final Pattern JSON_CODE_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private static final String PROMPT_TEMPLATE = CommonsUtils.getStringOfRes("aibot2/tool/UpsertRecord__ask.md");

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = JSON.parseObject(arguments);
        String file = args.getString("file");
        String content = args.getString("content");
        String entityName = args.getString("entity");
        String recordId = args.getString("recordId");
        boolean outputJson = args.getBooleanValue("outputJson");

        if (StringUtils.isBlank(file) && StringUtils.isBlank(content)) {
            throw new ToolException("文件或内容不能为空（需指定 file 或 content 参数之一）");
        }
        if (StringUtils.isBlank(entityName)) {
            throw new ToolException("实体名称不能为空");
        }

        String fileContent = StringUtils.isNotBlank(content)
                ? content
                : new FileData(file).toVector();

        Entity entity = ListEntities.resolveEntity(entityName);
        if (entity == null) {
            throw new ToolException("未知实体 : " + entityName);
        }

        String entityMetaDesc = buildEntityMetaDesc(entity);
        String prompt = Objects.requireNonNull(PROMPT_TEMPLATE).replace("{ENTITY_META_DESC}", entityMetaDesc);

        String aiResult = ChatManager.ask(fileContent, prompt);

        JSONObject recordJson = extractJson(aiResult);
        if (recordJson == null) {
            throw new ToolException("AI 解析失败，无法提取有效 JSON : " + CommonsUtils.maxstr(aiResult, 500));
        }

        ensureMetadata(recordJson, entity);

        if (outputJson) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "data", "message"},
                    new Object[]{"ok", recordJson, "文件内容已解析为 JSON"});
        }

        return saveRecord(recordJson, entity, recordId);
    }

    private JSONObject saveRecord(JSONObject recordJson, Entity entity, String recordId) {
        ID userId = UserContextHolder.getUser(true);
        if (userId == null) userId = UserService.SYSTEM_USER;

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

                Entity detailEntity = MetadataHelper.getEntity(detailJson.getJSONObject("metadata").getString("entity"));
                JSONObject cleanedDetail = RecordDataCleaner.cleanPostData(detailEntity, detailJson);
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
            throw new ToolException("保存记录失败 : " + CommonsUtils.getRootMessage(ex), ex);
        } finally {
            GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();
            GeneralEntityServiceContextHolder.isSkipSeriesValue(true);
        }

        String url = AppUtils.getContextPath("/app/redirect?id=" + record.getPrimary() + "&type=newtab");
        String message = isNew ? "记录已创建" : "记录已更新";
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
        return details[0];
    }

    private void ensureMetadata(JSONObject recordJson, Entity entity) {
        JSONObject metadata = recordJson.getJSONObject("metadata");
        if (metadata == null) {
            metadata = new JSONObject();
            recordJson.put("metadata", metadata);
        }
        if (StringUtils.isBlank(metadata.getString("entity"))) {
            metadata.put("entity", entity.getName());
        }
    }

    private JSONObject extractJson(String aiResult) {
        if (StringUtils.isBlank(aiResult)) return null;

        Matcher m = JSON_CODE_BLOCK.matcher(aiResult);
        if (m.find()) {
            try {
                return JSON.parseObject(m.group(1).trim());
            } catch (Exception ignored) {
            }
        }

        try {
            return JSON.parseObject(aiResult.trim());
        } catch (Exception ignored) {
        }

        m = JSON_OBJECT.matcher(aiResult);
        if (m.find()) {
            try {
                return JSON.parseObject(m.group());
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String buildEntityMetaDesc(Entity entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("实体: ").append(entity.getName())
                .append("（").append(EasyMetaFactory.getLabel(entity)).append("）\n");
        sb.append("字段列表:\n");

        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;
            appendFieldDesc(sb, field);
        }

        if (entity.getDetailEntity() != null) {
            for (Entity de : MetadataSorter.sortDetailEntities(entity)) {
                sb.append("\n明细实体: ").append(de.getName())
                        .append("（").append(EasyMetaFactory.getLabel(de)).append("）\n");
                sb.append("明细字段列表:\n");

                for (Field field : de.getFields()) {
                    if (MetadataHelper.isSystemField(field)) continue;
                    if (field.getType() == FieldType.REFERENCE && field.getReferenceEntity() == entity) continue;
                    appendFieldDesc(sb, field);
                }
            }
        }

        return sb.toString();
    }

    private void appendFieldDesc(StringBuilder sb, Field field) {
        DisplayType dt = EasyMetaFactory.getDisplayType(field);

        sb.append("  - ").append(field.getName())
                .append("（").append(EasyMetaFactory.getLabel(field)).append("）");
        sb.append(" 类型: ").append(dt.name());

        if (field.getType() == FieldType.REFERENCE || field.getType() == FieldType.REFERENCE_LIST) {
            Entity refEntity = field.getReferenceEntity();
            sb.append(" 引用实体: ").append(refEntity.getName())
                    .append("（").append(EasyMetaFactory.getLabel(refEntity)).append("）");
        }
        sb.append("\n");
    }
}
