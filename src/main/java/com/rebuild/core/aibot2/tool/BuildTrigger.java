/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerConfigService;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 新建触发器（仅管理员）。
 * 流程：Schema 校验（错误回传）→ 字段引用语义解析（标签转真实字段名）→ 用户确认 → 落库
 *
 * @author devezhao
 * @since 2026/8/16
 */
@Slf4j
public class BuildTrigger implements Tool, AdminGuard {

    // 允许通过 AI 创建的动作类型（排除 PROXYTRIGGERACTION/HOOKURL 等高危及强依赖其他配置的类型）
    private static final Set<ActionType> ALLOWED_ACTIONS = EnumSet.of(
            ActionType.FIELDWRITEBACK, ActionType.FIELDAGGREGATION, ActionType.GROUPAGGREGATION,
            ActionType.DATAVALIDATE, ActionType.SENDNOTIFICATION, ActionType.AUTOAPPROVAL,
            ActionType.AUTOASSIGN, ActionType.AUTOSHARE, ActionType.CREATEFEED);

    // 允许的触发时机（排除 TIMER，其需额外 cron 配置）
    private static final Set<TriggerWhen> ALLOWED_WHEN = EnumSet.of(
            TriggerWhen.CREATE, TriggerWhen.UPDATE, TriggerWhen.UPDATE_BEFORE, TriggerWhen.DELETE,
            TriggerWhen.ASSIGN, TriggerWhen.SHARE, TriggerWhen.UNSHARE,
            TriggerWhen.APPROVED, TriggerWhen.REVOKED, TriggerWhen.SUBMIT, TriggerWhen.REJECTED);

    private static final Map<TriggerWhen, String> WHEN_LABELS = new EnumMap<>(TriggerWhen.class);
    static {
        WHEN_LABELS.put(TriggerWhen.CREATE, "创建时");
        WHEN_LABELS.put(TriggerWhen.UPDATE, "更新时");
        WHEN_LABELS.put(TriggerWhen.UPDATE_BEFORE, "更新前");
        WHEN_LABELS.put(TriggerWhen.DELETE, "删除时");
        WHEN_LABELS.put(TriggerWhen.ASSIGN, "分配时");
        WHEN_LABELS.put(TriggerWhen.SHARE, "共享时");
        WHEN_LABELS.put(TriggerWhen.UNSHARE, "取消共享时");
        WHEN_LABELS.put(TriggerWhen.APPROVED, "审批通过时");
        WHEN_LABELS.put(TriggerWhen.REVOKED, "审批撤销时");
        WHEN_LABELS.put(TriggerWhen.SUBMIT, "审批提交时");
        WHEN_LABELS.put(TriggerWhen.REJECTED, "审批驳回/撤回时");
    }

    // 内容中的字段变量，如 {请假天数}
    private static final Pattern PATT_CONTENT_VAR = Pattern.compile("\\{([^{}]{1,60})}");

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        if (!UserHelper.isAdmin(user)) {
            throw new KnownToolException("仅管理员可配置触发器");
        }

        String entityIdent = args.getString("entity");
        if (StringUtils.isBlank(entityIdent)) {
            throw new KnownToolException("触发源实体 (entity) 不能为空");
        }
        Entity sourceEntity = ToolHelper.resolveEntity(entityIdent);
        if (sourceEntity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        String triggerName = args.getString("name");
        if (StringUtils.isBlank(triggerName)) {
            throw new KnownToolException("触发器名称 (name) 不能为空");
        }

        ActionType actionType = parseActionType(args.getString("actionType"));

        // 触发时机（位掩码）
        int whenMask = parseWhen(args.get("when"));

        JSONObject actionContent = args.getJSONObject("actionContent");
        if (actionContent == null || actionContent.isEmpty()) {
            throw new KnownToolException("动作内容 (actionContent) 不能为空，请先用 GetConfigSchema(schema=trigger-config) 获取其结构定义");
        }

        // 5. 附加过滤条件（仅 Schema 校验）
        JSONObject whenFilter = args.getJSONObject("whenFilter");
        if (whenFilter != null && !whenFilter.isEmpty()) {
            List<String> filterErrors = JsonSchemaValidator.validateErrors(JsonSchemaValidator.ADV_FILTER, whenFilter);
            if (filterErrors != null && !filterErrors.isEmpty()) {
                throw new KnownToolException("触发过滤条件 (whenFilter) 不符合规范 : " + ToolHelper.joinErrors(filterErrors)
                        + "。可用 GetConfigSchema(schema=adv-filter) 查看完整定义");
            }
        }

        // 6. Schema 校验（错误明细回传，供自修复重试）
        JSONObject schemaData = new JSONObject(true);
        schemaData.put("belongEntity", sourceEntity.getName());
        schemaData.put("when", whenMask);
        schemaData.put("actionType", actionType.name());
        schemaData.put("actionContent", actionContent);
        List<String> schemaErrors = JsonSchemaValidator.validateErrors(JsonSchemaValidator.TRIGGER_CONFIG, schemaData);
        if (schemaErrors != null && !schemaErrors.isEmpty()) {
            throw new KnownToolException("触发器配置不符合规范 : " + ToolHelper.joinErrors(schemaErrors)
                    + "。请修正后重试，可用 GetConfigSchema(schema=trigger-config) 查看完整定义");
        }

        // 7. 语义解析：字段标签/用户名等转真实标识
        resolveActionContent(sourceEntity, actionType, actionContent);

        // 同名检测：同实体已存在启用的同名触发器时，提醒用户新建后删除或禁用旧的
        Object[][] sameNameTriggers = Application.createQueryNoFilter(
                        "select configId from RobotTriggerConfig where belongEntity = ? and name = ? and isDisabled = 'F'")
                .setParameter(1, sourceEntity.getName())
                .setParameter(2, triggerName)
                .array();

        // 8. 触发器属重大配置，未确认时仅返回改动清单
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = buildChanges(sourceEntity, triggerName, whenMask, actionType, whenFilter, actionContent);
            if (sameNameTriggers.length > 0) {
                changes.put("注意", String.format("该实体已存在 %d 个启用的同名触发器，新建后请将旧触发器删除或禁用，避免重复执行",
                        sameNameTriggers.length));
            }
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。新建触发器会影响业务数据流转，请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        // 9. 落库（归属 AI 助手）
        Record record = EntityHelper.forNew(EntityHelper.RobotTriggerConfig, UserService.AIBOT_USER);
        record.setString("belongEntity", sourceEntity.getName());
        record.setString("name", triggerName);
        record.setInt("when", whenMask);
        record.setString("actionType", actionType.name());
        record.setString("actionContent", actionContent.toJSONString());
        record.setInt("priority", args.getIntValue("priority") > 0 ? args.getIntValue("priority") : 100);
        if (whenFilter != null && !whenFilter.isEmpty()) {
            record.setString("whenFilter", whenFilter.toJSONString());
        }

        record = Application.getBean(RobotTriggerConfigService.class).create(record);
        ID configId = record.getPrimary();

        log.info("Trigger created via AI : {} on {}", configId, sourceEntity.getName());

        String configUrl = AppUtils.getContextPath("/admin/robot/trigger/" + configId);
        String message = String.format("已成功创建触发器 [%s]（%s - %s），[点击查看触发器配置](%s)，请核对实际配置是否符合预期",
                triggerName, EasyMetaFactory.getLabel(sourceEntity), Language.L(actionType), configUrl);

        // 存在同名旧触发器时附删除/禁用提醒（附配置链接）
        if (sameNameTriggers.length > 0) {
            StringBuilder dupLinks = new StringBuilder();
            for (Object[] row : sameNameTriggers) {
                ID oldId = (ID) row[0];
                dupLinks.append(String.format("[同名触发器](%s)、", AppUtils.getContextPath("/admin/robot/trigger/" + oldId)));
            }
            message += String.format("。注意：该实体已存在启用的同名触发器 %s，请将旧触发器删除或禁用，避免重复执行", dupLinks);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "configId", "entity", "actionType", "name", "url", "message"},
                new Object[]{"ok", configId.toLiteral(), sourceEntity.getName(), actionType.name(), triggerName, configUrl, message});
    }

    /**
     * 解析动作类型
     *
     * @param typeStr
     * @return
     */
    private ActionType parseActionType(String typeStr) {
        if (StringUtils.isBlank(typeStr)) {
            throw new KnownToolException("动作类型 (actionType) 不能为空，可用值见本工具 actionType 参数说明");
        }

        ActionType actionType;
        try {
            actionType = ActionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new KnownToolException("无效动作类型 : " + typeStr + "，可用值: " + buildAllowedActionsDesc());
        }

        if (!ALLOWED_ACTIONS.contains(actionType)) {
            throw new KnownToolException("暂不支持通过 AI 创建该动作类型 : " + typeStr + "，可用值: " + buildAllowedActionsDesc());
        }
        return actionType;
    }

    /**
     * 可用动作描述，如 FIELDWRITEBACK(字段更新)
     *
     * @return
     */
    private String buildAllowedActionsDesc() {
        List<String> list = new ArrayList<>();
        for (ActionType t : ALLOWED_ACTIONS) {
            list.add(t.name() + "(" + Language.L(t) + ")");
        }
        return StringUtils.join(list, ", ");
    }

    /**
     * 解析触发时机为位掩码（支持数组或逗号分隔字符串）
     *
     * @param whenValue
     * @return
     */
    private int parseWhen(Object whenValue) {
        List<String> items = new ArrayList<>();
        if (whenValue instanceof JSONArray) {
            for (Object o : (JSONArray) whenValue) items.add(String.valueOf(o));
        } else if (whenValue != null && StringUtils.isNotBlank(whenValue.toString())) {
            for (String s : whenValue.toString().split("[,，]")) items.add(s);
        }

        int mask = 0;
        for (String item : items) {
            TriggerWhen tw;
            try {
                tw = TriggerWhen.valueOf(item.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new KnownToolException("无效触发时机 : " + item + "，可用值见本工具 when 参数说明");
            }
            if (!ALLOWED_WHEN.contains(tw)) {
                throw new KnownToolException("暂不支持该触发时机 : " + tw.name() + "，可用值见本工具 when 参数说明");
            }
            mask += tw.getMaskValue();
        }

        if (mask == 0) {
            throw new KnownToolException("触发时机 (when) 不能为空");
        }
        return mask;
    }

    /**
     * 语义解析动作内容：将字段标签、用户名等转真实字段名/ID（原地修改）
     *
     * @param sourceEntity
     * @param actionType
     * @param content
     */
    private void resolveActionContent(Entity sourceEntity, ActionType actionType, JSONObject content) {
        if (actionType == ActionType.FIELDWRITEBACK
                || actionType == ActionType.FIELDAGGREGATION
                || actionType == ActionType.GROUPAGGREGATION) {

            Entity target4Fields = resolveTargetEntity(sourceEntity, content);

            JSONArray items = content.getJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    if (item == null) continue;

                    String targetField = item.getString("targetField");
                    if (StringUtils.isNotBlank(targetField)) {
                        item.put("targetField", ToolHelper.resolveField(target4Fields, targetField).getName());
                    }

                    String sourceField = item.getString("sourceField");
                    if (StringUtils.isNotBlank(sourceField)) {
                        item.put("sourceField", ToolHelper.resolveFieldPath(sourceEntity, sourceField));
                    }
                }
            }

            JSONArray matchFields = content.getJSONArray("targetEntityMatchFields");
            if (matchFields != null) {
                for (int i = 0; i < matchFields.size(); i++) {
                    JSONObject mf = matchFields.get(i) instanceof JSONObject ? matchFields.getJSONObject(i) : null;
                    if (mf == null || StringUtils.isBlank(mf.getString("sourceField")) || StringUtils.isBlank(mf.getString("targetField"))) {
                        throw new KnownToolException("targetEntityMatchFields 每项必须为 {\"sourceField\":\"源实体字段\", \"targetField\":\"目标实体字段\"} 对象");
                    }
                    mf.put("sourceField", ToolHelper.resolveFieldPath(sourceEntity, mf.getString("sourceField")));
                    mf.put("targetField", ToolHelper.resolveField(target4Fields, mf.getString("targetField")).getName());
                }
            }

        } else if (actionType == ActionType.SENDNOTIFICATION) {
            int userType = content.getIntValue("userType");

            Object sendTo = content.get("sendTo");
            if (sendTo != null) {
                if (userType == 1 || userType == 0) {
                    // 内部用户：ID/用户全名/FIELD:字段
                    content.put("sendTo", resolveUserSelector(sourceEntity, sendTo, "sendTo"));
                } else if (userType == 2) {
                    // 外部人员字段
                    content.put("sendTo", ToolHelper.resolveField(sourceEntity, sendTo.toString()).getName());
                }
            }

            putIfResolved(content, "title", resolveContentVariables(sourceEntity, content.getString("title")));
            putIfResolved(content, "content", resolveContentVariables(sourceEntity, content.getString("content")));

        } else if (actionType == ActionType.AUTOASSIGN) {
            Object assignTo = content.get("assignTo");
            if (assignTo != null) {
                content.put("assignTo", resolveUserSelector(sourceEntity, assignTo, "assignTo"));
            }

        } else if (actionType == ActionType.AUTOSHARE) {
            Object shareTo = content.get("shareTo");
            if (shareTo != null) {
                content.put("shareTo", resolveUserSelector(sourceEntity, shareTo, "shareTo"));
            }

        } else if (actionType == ActionType.AUTOAPPROVAL) {
            String useApproval = content.getString("useApproval");
            if (StringUtils.isNotBlank(useApproval)) {
                content.put("useApproval", resolveApproval(useApproval, sourceEntity));
            }

        } else if (actionType == ActionType.CREATEFEED) {
            for (String fieldRef : new String[]{"scheduleTime", "relatedRecord", "postUser"}) {
                String v = content.getString(fieldRef);
                if (StringUtils.isNotBlank(v)) {
                    content.put(fieldRef, ToolHelper.resolveField(sourceEntity, v).getName());
                }
            }
            putIfResolved(content, "content", resolveContentVariables(sourceEntity, content.getString("content")));
        }
    }

    /**
     * 解析结果非空时才写入（避免存储 null 值）
     *
     * @param content
     * @param key
     * @param value
     */
    private void putIfResolved(JSONObject content, String key, String value) {
        if (StringUtils.isNotBlank(value)) content.put(key, value);
    }

    /**
     * 解析目标实体并返回用于解析 targetField 的实体
     *
     * @param sourceEntity
     * @param content
     * @return
     */
    private Entity resolveTargetEntity(Entity sourceEntity, JSONObject content) {
        String targetEntity = content.getString("targetEntity");
        if (StringUtils.isBlank(targetEntity)) {
            return sourceEntity;
        }

        // 自己更新自己
        if (TriggerAction.SOURCE_SELF.equals(targetEntity)) {
            return sourceEntity;
        }

        // $.实体名（任意实体，通过 targetEntityMatchFields 字段匹配）
        if (targetEntity.startsWith(TriggerAction.TARGET_ANY + ".")) {
            String entityIdent = targetEntity.substring(2);
            Entity target = ToolHelper.resolveEntity(entityIdent);
            if (target == null) {
                throw new KnownToolException("无效目标实体 (targetEntity) : " + targetEntity
                        + "。$. 后必须是目标实体名（如 $.SalesOrder）" + ToolHelper.suggestEntity(entityIdent)
                        + "；通过引用字段定位目标记录请使用「引用字段名.实体名」格式（如 SalesOrderId.SalesOrder）");
            }

            JSONArray matchFields = content.getJSONArray("targetEntityMatchFields");
            if (matchFields == null) matchFields = content.getJSONArray("groupFields");  // GROUPAGGREGATION
            if (matchFields == null || matchFields.isEmpty()) {
                throw new KnownToolException("字段匹配模式 ($.实体名) 必须提供 targetEntityMatchFields（每项为 {sourceField, targetField} 对象）");
            }

            content.put("targetEntity", TriggerAction.TARGET_ANY + "." + target.getName());
            return target;
        }

        if (targetEntity.contains(".")) {
            String[] parts = targetEntity.split("\\.", 2);
            Field refField = ToolHelper.resolveField(sourceEntity, parts[0]);
            if (refField.getType() != FieldType.REFERENCE) {
                throw new KnownToolException("targetEntity 的字段部分必须是引用字段 : " + parts[0]);
            }
            Entity target = ToolHelper.resolveEntity(parts[1]);
            if (target == null) {
                throw new KnownToolException("未知目标实体 : " + parts[1] + ToolHelper.suggestEntity(parts[1]));
            }
            content.put("targetEntity", refField.getName() + "." + target.getName());
            return target;
        }

        throw new KnownToolException("无效目标实体 (targetEntity) : " + targetEntity
                + "。格式应为「引用字段名.实体名」「$.实体名」（字段匹配模式）或 \"$PRIMARY$\"（源记录自己）");
    }

    /**
     * 解析用户选择器数组（UserSelector 标准格式，见 user-selector Schema）
     * 元素可为用户/部门/团队 ID、记录上的用户字段名/路径（无前缀）、OWNS、用户全名/用户名（自动转为 ID）
     *
     * @param sourceEntity
     * @param value
     * @param paramPath
     * @return
     */
    private JSONArray resolveUserSelector(Entity sourceEntity, Object value, String paramPath) {
        JSONArray arr = new JSONArray();
        if (value instanceof JSONArray) {
            arr.addAll((JSONArray) value);
        } else {
            arr.add(value);
        }

        JSONArray resolved = new JSONArray();
        for (Object o : arr) {
            String v = String.valueOf(o).trim();

            if (ID.isId(v) || "OWNS".equals(v)) {
                resolved.add(v);
                continue;
            }

            // 字段名/路径（跨引用实体），运行时由 UserHelper.parseUsers 取值
            String fieldPath = null;
            try {
                fieldPath = ToolHelper.resolveFieldPath(sourceEntity, v);
            } catch (KnownToolException ignored) {
                // 非字段，继续尝试按用户名解析
            }
            if (fieldPath != null && MetadataHelper.getLastJoinField(sourceEntity, fieldPath, true) != null) {
                resolved.add(fieldPath);
                continue;
            }

            ID uid = ToolHelper.resolveUser(v);
            if (uid != null) {
                resolved.add(uid.toLiteral());
            } else {
                throw new KnownToolException(paramPath + " 无法解析 : " + v
                        + "。请使用用户 ID、用户全名，或记录上的用户字段名/路径（如 owningUser、相关客户.负责人），"
                        + "详见 GetConfigSchema(schema=user-selector)");
            }
        }
        return resolved;
    }

    /**
     * 解析审批流程（支持 ID、流程名称）
     *
     * @param ident
     * @param sourceEntity
     * @return
     */
    private String resolveApproval(String ident, Entity sourceEntity) {
        if (ID.isId(ident)) return ident;

        Object[][] rows = Application.createQueryNoFilter(
                        "select configId,name from RobotApprovalConfig where belongEntity = ? and isDisabled = 'F'")
                .setParameter(1, sourceEntity.getName())
                .array();

        List<Object[]> fuzzy = new ArrayList<>();
        for (Object[] row : rows) {
            ID configId = (ID) row[0];
            String name = (String) row[1];
            if (ident.equals(name)) return configId.toLiteral();
            if (StringUtils.containsIgnoreCase(name, ident) || StringUtils.containsIgnoreCase(ident, name)) {
                fuzzy.add(row);
            }
        }

        if (fuzzy.size() == 1) return ((ID) fuzzy.get(0)[0]).toLiteral();

        List<String> names = new ArrayList<>();
        for (Object[] row : fuzzy) names.add((String) row[1]);
        String suggest = names.isEmpty() ? "" : "，可用流程: " + StringUtils.join(names, ", ");
        throw new KnownToolException("未找到匹配的审批流程 : " + ident + suggest);
    }

    /**
     * 替换内容中的字段变量（标签转真实字段名）。
     * 注意：字段变量最终须为英文字段名（运行时仅识别 ASCII 字段名），如 {请假天数} 会被替换
     *
     * @param sourceEntity
     * @param contentText
     * @return
     */
    private String resolveContentVariables(Entity sourceEntity, String contentText) {
        if (StringUtils.isBlank(contentText)) return contentText;

        Matcher m = PATT_CONTENT_VAR.matcher(contentText);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            if (!sourceEntity.containsField(token)) {
                try {
                    token = ToolHelper.resolveField(sourceEntity, token).getName();
                } catch (ToolException ignore) {
                    // 保留原文（可能是 {ID}/{NOW} 等系统变量）
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement("{" + token + "}"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 构建改动清单（供用户确认）
     *
     * @param sourceEntity
     * @param triggerName
     * @param whenMask
     * @param actionType
     * @param whenFilter
     * @param actionContent
     * @return
     */
    private JSONObject buildChanges(Entity sourceEntity, String triggerName, int whenMask,
                                    ActionType actionType, JSONObject whenFilter, JSONObject actionContent) {
        JSONObject changes = new JSONObject(true);
        changes.put("操作", "新建触发器");
        changes.put("触发实体", EasyMetaFactory.getLabel(sourceEntity));
        changes.put("触发器名称", triggerName);
        changes.put("触发时机", buildWhenLabel(whenMask));
        changes.put("动作类型", Language.L(actionType));
        changes.put("动作内容", actionContent);
        if (whenFilter != null && !whenFilter.isEmpty()) {
            changes.put("附加条件", whenFilter);
        }
        return changes;
    }

    /**
     * 触发时机描述
     *
     * @param whenMask
     * @return
     */
    private String buildWhenLabel(int whenMask) {
        List<String> labels = new ArrayList<>();
        for (TriggerWhen tw : ALLOWED_WHEN) {
            if ((whenMask & tw.getMaskValue()) != 0) {
                labels.add(WHEN_LABELS.get(tw));
            }
        }
        return StringUtils.join(labels, "、");
    }
}
