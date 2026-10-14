/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.support.License;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 新建实体（仅管理员）
 *
 * @author devezhao
 * @since 2026/8/10
 */
@Slf4j
public class BuildEntity implements Tool, AdminGuard {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String entityLabel = args.getString("entityLabel");
        if (StringUtils.isBlank(entityLabel)) {
            throw new KnownToolException("实体名称 (entityLabel) 不能为空");
        }

        String entityName = StringUtils.trimToNull(args.getString("entityName"));

        // 主实体（可选，指定后创建为明细实体）
        String mainEntity = args.getString("mainEntity");
        if (StringUtils.isNotBlank(mainEntity)) {
            Entity useMain = ToolHelper.resolveEntity(mainEntity);
            if (useMain == null) {
                throw new KnownToolException("无效主实体 : " + mainEntity + ToolHelper.suggestEntity(mainEntity));
            }
            if (useMain.getMainEntity() != null) {
                throw new KnownToolException("明细实体不能作为主实体");
            }
            // 免费版一个主实体只能挂载一个明细实体
            if (useMain.getDetailEntity() != null && !License.isCommercial()) {
                throw new KnownToolException("免费版不支持多明细功能，一个主实体只能挂载一个明细实体");
            }
            mainEntity = useMain.getName();
        }

        boolean haveNameField = args.getBooleanValue("nameField");
        boolean haveSeriesField = args.getBooleanValue("seriesField");

        JSONArray fields = args.getJSONArray("fields");
        if (fields != null && !fields.isEmpty()) {
            validateFields(fields);
        }

        // 实体是系统核心，未确认时仅返回改动清单，须用户二次确认后才创建
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = buildChanges(entityName, entityLabel, mainEntity, haveNameField, haveSeriesField, fields, args.getString("comments"));
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。新建实体属于重大改动，请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        if (fields != null && !fields.isEmpty() && ToolDefs.isToolDisabled("BuildField", ToolDefs.currentAgentTools())) {
            throw new KnownToolException("本工具依赖 BuildField，但该工具当前不可用");
        }

        entityName = new Entity2Schema(UserContextHolder.getUser()).createEntity(
                entityName, entityLabel, args.getString("comments"), mainEntity, haveNameField, haveSeriesField);

        String entityUrl = AppUtils.getContextPath("/admin/entity/" + entityName + "/base");

        if (fields != null && !fields.isEmpty()) {
            return createFields(entityName, entityLabel, mainEntity != null, fields, entityUrl);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "entity", "label", "url", "message"},
                new Object[]{"ok", entityName, entityLabel, entityUrl,
                        String.format("已成功创建%s [%s](%s)，[点击前往实体管理](%s)，请将此链接展示给用户，以便其核对并配置表单和布局",
                                mainEntity != null ? "明细实体" : "实体", entityLabel, entityName, entityUrl)});
    }

    /**
     * 构建改动清单（供用户确认）
     *
     * @param entityName
     * @param entityLabel
     * @param mainEntity
     * @param haveNameField
     * @param haveSeriesField
     * @param fields
     * @param comments
     * @return
     */
    private JSONObject buildChanges(String entityName, String entityLabel, String mainEntity,
                                    boolean haveNameField, boolean haveSeriesField, JSONArray fields, String comments) {
        JSONObject changes = new JSONObject(true);
        changes.put("操作", mainEntity != null
                ? "新建明细实体（主实体：" + EasyMetaFactory.getLabel(MetadataHelper.getEntity(mainEntity)) + "）"
                : "新建实体");
        changes.put("实体名称", entityLabel);
        if (entityName != null) changes.put("内部标识", entityName);
        if (haveNameField) changes.put("同时创建", "名称字段" + (haveSeriesField ? "、自动编号字段" : ""));
        else if (haveSeriesField) changes.put("同时创建", "自动编号字段");
        if (fields != null && !fields.isEmpty()) {
            List<String> fieldDescs = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                fieldDescs.add(f.getString("fieldLabel") + "(" + f.getString("type") + ")");
            }
            changes.put("字段", StringUtils.join(fieldDescs, "、"));
        }
        if (StringUtils.isNotBlank(comments)) changes.put("描述", comments);

        // 提示同名实体，避免用户误操作
        for (Entity e : MetadataHelper.getEntities()) {
            if (entityLabel.equalsIgnoreCase(EasyMetaFactory.getLabel(e))) {
                changes.put("注意", "已存在同名实体「" + EasyMetaFactory.getLabel(e) + "」，创建后将自动区分命名");
                break;
            }
        }
        return changes;
    }

    /**
     * 校验字段规格
     *
     * @param fields
     */
    private void validateFields(JSONArray fields) {
        Set<String> fieldLabels = new HashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String fieldLabel = field.getString("fieldLabel");
            if (StringUtils.isBlank(fieldLabel)) {
                throw new KnownToolException("第 " + (i + 1) + " 个字段的 fieldLabel 不能为空");
            }
            String type = field.getString("type");
            if (StringUtils.isBlank(type)) {
                throw new KnownToolException("字段「" + fieldLabel + "」的 type 不能为空");
            }
            if (!fieldLabels.add(fieldLabel)) {
                throw new KnownToolException("字段名称重复 : " + fieldLabel);
            }
        }
    }

    /**
     * 批量创建字段，单字段失败不中断
     *
     * @param entityName
     * @param entityLabel
     * @param isDetail
     * @param fields
     * @param entityUrl
     * @return
     */
    private JSONObject createFields(String entityName, String entityLabel, boolean isDetail, JSONArray fields, String entityUrl) {
        int fieldsCreated = 0;
        JSONArray failedItems = new JSONArray();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject fieldSpec = fields.getJSONObject(i);
            String fieldLabel = fieldSpec.getString("fieldLabel");
            try {
                String fieldResult = ToolDefs.execute("BuildField", buildFieldArgs(fieldSpec, entityName));
                JSONObject resultJson = JSON.parseObject(fieldResult);
                if (!"ok".equals(resultJson.getString("status"))) {
                    throw new KnownToolException(resultJson.getString("message"));
                }
                fieldsCreated++;
            } catch (Exception ex) {
                String error = CommonsUtils.getRootMessage(ex);
                log.warn("BuildEntity: 字段创建失败 [{}.{}]: {}", entityLabel, fieldLabel, error);
                JSONObject failed = new JSONObject(true);
                failed.put("fieldLabel", fieldLabel);
                failed.put("error", error);
                failedItems.add(failed);
            }
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("已成功创建%s [%s](%s) 及 %d 个字段",
                isDetail ? "明细实体" : "实体", entityLabel, entityName, fieldsCreated));
        if (!failedItems.isEmpty()) {
            message.append(String.format("（%d 项失败）", failedItems.size()));
        }
        message.append(String.format("，[点击前往实体管理](%s)，请将此链接展示给用户，以便其核对并配置表单和布局", entityUrl));
        for (int i = 0; i < failedItems.size(); i++) {
            JSONObject failed = failedItems.getJSONObject(i);
            message.append(String.format("\n- 字段「%s」创建失败: %s", failed.getString("fieldLabel"), failed.getString("error")));
        }

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("entity", entityName);
        result.put("label", entityLabel);
        result.put("url", entityUrl);
        result.put("fieldsCreated", fieldsCreated);
        if (!failedItems.isEmpty()) {
            result.put("failedItems", failedItems);
        }
        result.put("message", message.toString());
        return result;
    }

    /**
     * 构建 BuildField 参数
     *
     * @param fieldSpec
     * @param entityName
     * @return
     */
    private String buildFieldArgs(JSONObject fieldSpec, String entityName) {
        JSONObject fieldArgs = new JSONObject();
        fieldArgs.put("entity", entityName);
        fieldArgs.put("fieldLabel", fieldSpec.getString("fieldLabel"));
        fieldArgs.put("type", fieldSpec.getString("type"));

        String fieldName = fieldSpec.getString("fieldName");
        if (StringUtils.isNotBlank(fieldName)) {
            fieldArgs.put("fieldName", fieldName);
        }

        String refEntity = fieldSpec.getString("refEntity");
        if (StringUtils.isNotBlank(refEntity)) {
            fieldArgs.put("refEntity", refEntity);
        }

        JSONArray options = fieldSpec.getJSONArray("options");
        if (options != null) {
            fieldArgs.put("options", options);
        }

        String classification = fieldSpec.getString("classification");
        if (StringUtils.isNotBlank(classification)) {
            fieldArgs.put("classification", classification);
        }

        String comments = fieldSpec.getString("comments");
        if (StringUtils.isNotBlank(comments)) {
            fieldArgs.put("comments", comments);
        }

        fieldArgs.put("confirmed", true);
        return fieldArgs.toJSONString();
    }
}
