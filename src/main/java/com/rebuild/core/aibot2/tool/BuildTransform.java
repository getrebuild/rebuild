/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.configuration.general.TransformConfigService;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 新建记录转换配置（仅管理员）。
 * 流程：参数校验 → 字段标签转真实字段名 → 用户确认 → 落库
 *
 * @author devezhao
 * @since 2026/8/19
 */
@Slf4j
public class BuildTransform implements Tool, AdminGuard {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        // 源实体
        String sourceEntityIdent = args.getString("sourceEntity");
        if (StringUtils.isBlank(sourceEntityIdent)) {
            throw new KnownToolException("源实体 (sourceEntity) 不能为空");
        }
        Entity sourceEntity = ToolHelper.resolveEntity(sourceEntityIdent);
        if (sourceEntity == null) {
            throw new KnownToolException("未知源实体 : " + sourceEntityIdent + ToolHelper.suggestEntity(sourceEntityIdent));
        }

        // 目标实体
        String targetEntityIdent = args.getString("targetEntity");
        if (StringUtils.isBlank(targetEntityIdent)) {
            throw new KnownToolException("目标实体 (targetEntity) 不能为空");
        }
        Entity targetEntity = ToolHelper.resolveEntity(targetEntityIdent);
        if (targetEntity == null) {
            throw new KnownToolException("未知目标实体 : " + targetEntityIdent + ToolHelper.suggestEntity(targetEntityIdent));
        }

        // 名称
        String name = args.getString("name");
        if (StringUtils.isBlank(name)) {
            throw new KnownToolException("转换配置名称 (name) 不能为空");
        }

        // 字段映射
        JSONObject fieldsMapping = args.getJSONObject("fieldsMapping");
        if (fieldsMapping == null || fieldsMapping.isEmpty()) {
            throw new KnownToolException("字段映射 (fieldsMapping) 不能为空，请至少配置一个字段映射");
        }

        // 语义解析：将字段标签转为真实字段名
        JSONObject resolvedMapping = resolveFieldsMapping(sourceEntity, targetEntity, fieldsMapping);

        // 明细字段映射
        JSONArray fieldsMappingDetail = args.getJSONArray("fieldsMappingDetail");
        JSONArray resolvedDetails = null;
        if (fieldsMappingDetail != null && !fieldsMappingDetail.isEmpty()) {
            resolvedDetails = resolveFieldsMappingDetail(sourceEntity, targetEntity, fieldsMappingDetail);
        }

        // 过滤条件
        JSONObject useFilter = args.getJSONObject("useFilter");
        if (useFilter != null && !useFilter.isEmpty()) {
            ToolHelper.validateFilter(sourceEntity, useFilter);
        }

        // 构建 config JSON
        JSONObject config = new JSONObject(true);
        config.put("fieldsMapping", resolvedMapping);
        if (resolvedDetails != null) {
            config.put("fieldsMappingDetails", resolvedDetails);
        }
        if (useFilter != null && !useFilter.isEmpty()) {
            config.put("useFilter", useFilter);
        }
        if (args.getBooleanValue("importsMode")) {
            config.put("importsMode", true);
        }

        // 二次确认
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = new JSONObject(true);
            changes.put("操作", "新建记录转换配置");
            changes.put("源实体", EasyMetaFactory.getLabel(sourceEntity));
            changes.put("目标实体", EasyMetaFactory.getLabel(targetEntity));
            changes.put("配置名称", name);
            changes.put("字段映射", resolvedMapping);
            if (resolvedDetails != null) {
                changes.put("明细映射", resolvedDetails);
            }
            if (useFilter != null && !useFilter.isEmpty()) {
                changes.put("过滤条件", useFilter);
            }
            if (args.getBooleanValue("importsMode")) {
                changes.put("明细导入模式", "是");
            }
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        // 落库
        Record record = EntityHelper.forNew(EntityHelper.TransformConfig, UserService.AIBOT_USER);
        record.setString("belongEntity", sourceEntity.getName());
        record.setString("targetEntity", targetEntity.getName());
        record.setString("name", name);
        record.setString("config", config.toJSONString());
        record = Application.getBean(TransformConfigService.class).create(record);
        ID configId = record.getPrimary();

        log.info("TransformConfig created via AI : {} {} -> {}", configId, sourceEntity.getName(), targetEntity.getName());

        String configUrl = AppUtils.getContextPath("/admin/robot/transform/" + configId);
        String message = String.format("已成功创建记录转换配置 [%s]（%s → %s），[点击查看转换配置](%s)，请将此链接展示给用户，以便其核对字段映射是否符合预期",
                name, EasyMetaFactory.getLabel(sourceEntity), EasyMetaFactory.getLabel(targetEntity), configUrl);

        return JSONUtils.toJSONObject(
                new String[]{"status", "configId", "sourceEntity", "targetEntity", "name", "url", "message"},
                new Object[]{"ok", configId.toLiteral(), sourceEntity.getName(), targetEntity.getName(), name, configUrl, message});
    }

    /**
     * 解析主记录字段映射：key 为目标字段（标签转真实名），value 为源字段（标签转真实名）或固定值
     *
     * @param sourceEntity
     * @param targetEntity
     * @param fieldsMapping
     * @return
     */
    private JSONObject resolveFieldsMapping(Entity sourceEntity, Entity targetEntity, JSONObject fieldsMapping) {
        JSONObject resolved = new JSONObject(true);
        for (Map.Entry<String, Object> entry : fieldsMapping.entrySet()) {
            String targetFieldKey = entry.getKey();
            // 跳过元数据
            if ("_".equals(targetFieldKey)) continue;

            // 解析目标字段
            String targetFieldName = ToolHelper.resolveField(targetEntity, targetFieldKey).getName();

            // 解析值
            Object value = entry.getValue();
            if (value instanceof String) {
                String strValue = (String) value;
                if (StringUtils.isBlank(strValue)) {
                    resolved.put(targetFieldName, strValue);
                    continue;
                }
                // 尝试作为源字段解析
                try {
                    String resolvedSourceField = ToolHelper.resolveFieldPath(sourceEntity, strValue);
                    resolved.put(targetFieldName, resolvedSourceField);
                } catch (KnownToolException notAField) {
                    // 非字段名，作为固定值保留
                    resolved.put(targetFieldName, strValue);
                }
            } else {
                // 非字符串（数字、布尔等）作为固定值
                resolved.put(targetFieldName, value);
            }
        }
        return resolved;
    }

    /**
     * 解析明细字段映射：将 {source, target, fieldsMapping} 转为内部格式（"_" 元数据 + 字段映射平铺）
     *
     * @param sourceMainEntity
     * @param targetMainEntity
     * @param fieldsMappingDetail
     * @return
     */
    private JSONArray resolveFieldsMappingDetail(Entity sourceMainEntity, Entity targetMainEntity, JSONArray fieldsMappingDetail) {
        JSONArray resolved = new JSONArray();
        for (int i = 0; i < fieldsMappingDetail.size(); i++) {
            JSONObject item = fieldsMappingDetail.getJSONObject(i);
            if (item == null) continue;

            String sourceDetailName = item.getString("source");
            String targetDetailName = item.getString("target");
            JSONObject detailMapping = item.getJSONObject("fieldsMapping");

            if (StringUtils.isBlank(sourceDetailName) || StringUtils.isBlank(targetDetailName) || detailMapping == null) {
                throw new KnownToolException("fieldsMappingDetail 每项必须含 source、target、fieldsMapping 三个字段");
            }

            Entity sourceDetailEntity = ToolHelper.resolveEntity(sourceDetailName);
            if (sourceDetailEntity == null) {
                throw new KnownToolException("未知源明细实体 : " + sourceDetailName + ToolHelper.suggestEntity(sourceDetailName));
            }
            Entity targetDetailEntity = ToolHelper.resolveEntity(targetDetailName);
            if (targetDetailEntity == null) {
                throw new KnownToolException("未知目标明细实体 : " + targetDetailName + ToolHelper.suggestEntity(targetDetailName));
            }

            // 构建内部格式：_ 元数据 + 字段映射平铺
            JSONObject resolvedItem = new JSONObject(true);
            JSONObject meta = new JSONObject(true);
            meta.put("source", sourceDetailEntity.getName());
            meta.put("target", targetDetailEntity.getName());
            resolvedItem.put("_", meta);

            // 解析字段映射
            JSONObject resolvedMapping = resolveFieldsMapping(sourceDetailEntity, targetDetailEntity, detailMapping);
            resolvedItem.putAll(resolvedMapping);

            resolved.add(resolvedItem);
        }
        return resolved;
    }
}
