/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.service.trigger.TriggerHelper.tryParseTargetEntity;

/**
 * 查询现有配置（触发器/审批流程/记录转换/报表模板）
 *
 * @author devezhao
 * @since 2026/11/6
 */
public class ListConfigs implements Tool, AdminGuard {

    private static final String TYPE_TRIGGER = "trigger";
    private static final String TYPE_APPROVAL = "approval";
    private static final String TYPE_TRANSFORM = "transform";
    private static final String TYPE_REPORT_TEMPLATE = "report-template";

    private static final String TYPE_ALL = "trigger、approval、transform、report-template";

    // 不限定实体时的返回上限，避免大量配置撑爆模型上下文
    private static final int MAX_CONFIGS = 200;
    // 单条配置大字段（动作内容/流程定义/映射配置/模板内容）的返回上限
    private static final int MAX_CONTENT_LENGTH = 4000;

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
        WHEN_LABELS.put(TriggerWhen.TIMER, "定时");
        WHEN_LABELS.put(TriggerWhen.SUBMIT, "审批提交时");
        WHEN_LABELS.put(TriggerWhen.REJECTED, "审批驳回/撤回时");
    }

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        final ID user = UserContextHolder.getUser();
        if (!UserHelper.isAdmin(user)) {
            throw new KnownToolException("仅管理员可使用此工具");
        }

        String configType = args.getString("configType");
        if (StringUtils.isBlank(configType)) {
            throw new KnownToolException("configType 不能为空，可用值 " + TYPE_ALL);
        }

        Entity sourceEntity = null;
        String entityIdent = args.getString("entity");
        if (StringUtils.isNotBlank(entityIdent)) {
            sourceEntity = ToolHelper.resolveEntity(entityIdent);
            if (sourceEntity == null) {
                throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
            }
        }

        switch (configType) {
            case TYPE_TRIGGER:
                return listTriggers(sourceEntity);
            case TYPE_APPROVAL:
                return listApprovals(sourceEntity);
            case TYPE_TRANSFORM:
                return listTransforms(sourceEntity);
            case TYPE_REPORT_TEMPLATE:
                return listReportTemplates(sourceEntity);
            default:
                throw new KnownToolException("未知的配置类型 : " + configType + "，可用值 " + TYPE_ALL);
        }
    }

    /**
     * @param sourceEntity
     * @return
     */
    private JSONObject listTriggers(Entity sourceEntity) {
        Object[][] rows = queryConfigs(
                "select configId,name,belongEntity,when,whenFilter,actionType,actionContent,priority,isDisabled from RobotTriggerConfig",
                sourceEntity, "priority desc, modifiedOn desc");

        int enabled = 0;
        JSONArray configs = new JSONArray();
        for (Object[] row : rows) {
            ID configId = (ID) row[0];
            String belongEntity = (String) row[2];
            // isDisabled 为 char('T'/'F')，查询结果为 Boolean 类型
            boolean isDisabled = ObjectUtils.toBool(row[8]);
            if (!isDisabled) enabled++;

            JSONObject t = buildCommonItem(configId, row[1], belongEntity, isDisabled,
                    "/admin/robot/trigger/" + configId);
            t.put("priority", row[7]);

            buildWhen(t, ObjectUtils.toInt(row[3], 0));
            buildActionType(t, (String) row[5]);

            t.put("whenFilter", ToolHelper.parseJsonOrRaw((String) row[4]));
            String rawActionContent = (String) row[6];
            putContent(t, "actionContent", rawActionContent);
            buildTargetEntity(t, rawActionContent, belongEntity);

            configs.add(t);
        }

        String message = String.format("共 %d 个触发器（启用 %d 个，停用 %d 个）。"
                        + "请基于返回的完整配置分析后向用户报告：按用户关心的操作筛选 when 中含对应时机的触发器并结合 whenFilter 判断触发条件；"
                        + "targetEntity 已按动作类型解析出受影响的实体（含记录转换的实际目标实体），沿它继续用本工具查询其上的触发器以梳理级联影响，字段级影响再看 actionContent；"
                        + "结合 ListEntities 核对配置中引用的字段是否仍然存在，指出无效引用、重复触发器、触发时机不当等问题。"
                        + "向用户展示触发链路时可用 ```mermaid 代码块绘制流程图。修改现有触发器请直接引导用户到触发器配置页操作（见各触发器的 url 字段）",
                rows.length, enabled, rows.length - enabled);
        return buildResult(TYPE_TRIGGER, rows.length, enabled, configs, message);
    }

    /**
     * @param sourceEntity
     * @return
     */
    private JSONObject listApprovals(Entity sourceEntity) {
        Object[][] rows = queryConfigs(
                "select configId,name,belongEntity,flowDefinition,isDisabled from RobotApprovalConfig",
                sourceEntity, "modifiedOn desc");

        int enabled = 0;
        JSONArray configs = new JSONArray();
        for (Object[] row : rows) {
            boolean isDisabled = ObjectUtils.toBool(row[4]);
            if (!isDisabled) enabled++;

            ID configId = (ID) row[0];
            JSONObject t = buildCommonItem(configId, row[1], (String) row[2], isDisabled,
                    "/admin/robot/approval/" + configId);
            putContent(t, "flowDefinition", (String) row[3]);

            configs.add(t);
        }

        String message = String.format("共 %d 个审批流程（启用 %d 个，停用 %d 个）。"
                        + "请基于 flowDefinition 分析后向用户报告：梳理审批节点顺序、各节点的审批人与审批方式，以及节点生效条件；"
                        + "结合 ListEntities 核对流程中引用的字段是否仍然存在，指出无效引用、流程设计不当等问题。"
                        + "修改现有审批流程请直接引导用户到审批流程配置页操作（见各流程的 url 字段）",
                rows.length, enabled, rows.length - enabled);
        return buildResult(TYPE_APPROVAL, rows.length, enabled, configs, message);
    }

    /**
     * @param sourceEntity
     * @return
     */
    private JSONObject listTransforms(Entity sourceEntity) {
        Object[][] rows = queryConfigs(
                "select configId,name,belongEntity,targetEntity,config,isDisabled from TransformConfig",
                sourceEntity, "modifiedOn desc");

        int enabled = 0;
        JSONArray configs = new JSONArray();
        for (Object[] row : rows) {
            boolean isDisabled = ObjectUtils.toBool(row[5]);
            if (!isDisabled) enabled++;

            ID configId = (ID) row[0];
            JSONObject t = buildCommonItem(configId, row[1], (String) row[2], isDisabled,
                    "/admin/robot/transform/" + configId);

            String targetEntity = (String) row[3];
            t.put("targetEntity", targetEntity);
            if (MetadataHelper.containsEntity(targetEntity)) {
                t.put("targetEntityLabel", EasyMetaFactory.getLabel(targetEntity));
            } else {
                // 转换目标实体可能已被删除
                t.put("targetEntityMissing", true);
            }

            putContent(t, "config", (String) row[4]);

            configs.add(t);
        }

        String message = String.format("共 %d 个记录转换（启用 %d 个，停用 %d 个）。"
                        + "请基于 config 中的字段映射分析后向用户报告：梳理源实体到目标实体的字段对应关系与取值方式；"
                        + "结合 ListEntities 核对映射中引用的字段是否仍然存在，指出无效引用等问题；"
                        + "哪些触发器的记录转换动作引用了该转换，可用本工具（configType=trigger）查询后按 actionContent 中的引用核对。"
                        + "修改现有记录转换请直接引导用户到记录转换配置页操作（见各转换的 url 字段）",
                rows.length, enabled, rows.length - enabled);
        return buildResult(TYPE_TRANSFORM, rows.length, enabled, configs, message);
    }

    /**
     * @param sourceEntity
     * @return
     */
    private JSONObject listReportTemplates(Entity sourceEntity) {
        Object[][] rows = queryConfigs(
                "select configId,name,belongEntity,templateFile,templateContent,templateType,extraDefinition,isDisabled from DataReportConfig",
                sourceEntity, "modifiedOn desc");

        int enabled = 0;
        JSONArray configs = new JSONArray();
        for (Object[] row : rows) {
            boolean isDisabled = ObjectUtils.toBool(row[7]);
            if (!isDisabled) enabled++;

            ID configId = (ID) row[0];
            JSONObject t = buildCommonItem(configId, row[1], (String) row[2], isDisabled,
                    "/admin/data/report-template/design?id=" + configId);

            buildTemplateType(t, ObjectUtils.toInt(row[5], DataReportManager.TYPE_RECORD));
            t.put("templateFile", row[3]);
            putContent(t, "templateContent", (String) row[4]);
            t.put("extraDefinition", ToolHelper.parseJsonOrRaw((String) row[6]));

            configs.add(t);
        }

        String message = String.format("共 %d 个报表模板（启用 %d 个，停用 %d 个）。"
                        + "请基于返回内容分析后向用户报告：说明各模板的类型与数据来源实体，templateContent 为网页模板内容（超长已截断），基于 templateFile 的 Excel/Word 模板无此内容；"
                        + "模板的变量写法与预置变量可调 GetConfigSchema（schema=report-template）获取参考，结合 ListEntities 核对模板中引用的字段是否仍然存在。"
                        + "修改现有报表模板请直接引导用户到报表模板设计页操作（见各模板的 url 字段）",
                rows.length, enabled, rows.length - enabled);
        return buildResult(TYPE_REPORT_TEMPLATE, rows.length, enabled, configs, message);
    }

    /**
     * 按实体过滤查询配置，统一上限截断
     *
     * @param selectFrom
     * @param sourceEntity
     * @param orderBy
     * @return
     */
    private Object[][] queryConfigs(String selectFrom, Entity sourceEntity, String orderBy) {
        String sql = selectFrom;
        if (sourceEntity != null) sql += " where belongEntity = ?";
        sql += " order by " + orderBy;

        Query query = Application.createQueryNoFilter(sql).setLimit(MAX_CONFIGS);
        if (sourceEntity != null) query = query.setParameter(1, sourceEntity.getName());
        return query.array();
    }

    /**
     * 各类型配置的公共字段
     *
     * @param configId
     * @param name
     * @param belongEntity
     * @param isDisabled
     * @param url
     * @return
     */
    private JSONObject buildCommonItem(ID configId, Object name, String belongEntity, boolean isDisabled, String url) {
        JSONObject item = new JSONObject(true);
        item.put("id", configId.toLiteral());
        item.put("name", name);
        item.put("url", AppUtils.getContextPath(url));
        item.put("isDisabled", isDisabled);
        item.put("entity", belongEntity);
        if (MetadataHelper.containsEntity(belongEntity)) {
            item.put("entityLabel", EasyMetaFactory.getLabel(MetadataHelper.getEntity(belongEntity)));
        } else {
            // 所属实体可能已被删除
            item.put("entityMissing", true);
        }
        return item;
    }

    /**
     * 大字段统一截断保护，超长时仅保留可读前缀并标记
     *
     * @param item
     * @param key
     * @param rawContent
     */
    private void putContent(JSONObject item, String key, String rawContent) {
        if (rawContent != null && rawContent.length() > MAX_CONTENT_LENGTH) {
            item.put(key, CommonsUtils.maxstr(rawContent, MAX_CONTENT_LENGTH));
            item.put(key + "Truncated", true);
        } else {
            item.put(key, ToolHelper.parseJsonOrRaw(rawContent));
        }
    }

    /**
     * @param configType
     * @param count
     * @param enabled
     * @param configs
     * @param message
     * @return
     */
    private JSONObject buildResult(String configType, int count, int enabled, JSONArray configs, String message) {
        final boolean truncated = count >= MAX_CONFIGS;

        JSONObject result = new JSONObject(true);
        result.put("status", "ok");
        result.put("configType", configType);
        result.put("count", count);
        result.put("enabled", enabled);
        result.put("disabled", count - enabled);
        if (truncated) result.put("truncated", true);
        result.put("configs", configs);
        if (truncated) {
            message = String.format("结果已截断至前 %d 条，请用 entity 参数缩小范围。", MAX_CONFIGS) + message;
        }
        result.put("message", message);
        return result;
    }

    /**
     * 解析动作影响的目标实体（同 TriggerDiagramming），供级联影响分析。
     * 字段更新/聚合为 targetEntity，记录转换解析出转换目标实体，审批流/报表模板无目标实体
     *
     * @param t
     * @param actionContent
     * @param sourceEntity
     */
    private void buildTargetEntity(JSONObject t, String actionContent, String sourceEntity) {
        if (StringUtils.isBlank(actionContent)) return;

        String[] targetEntity = tryParseTargetEntity(actionContent, sourceEntity);
        if (targetEntity == null || targetEntity[0] == null) return;

        if (!ID.isId(targetEntity[0])) {
            t.put("targetEntity", targetEntity[0]);
            t.put("targetEntityLabel", targetEntity[1]);
            return;
        }

        ID targetId = ID.valueOf(targetEntity[0]);
        if (targetId.getEntityCode() == EntityHelper.TransformConfig) {
            try {
                ConfigBean cb = TransformManager.instance.getTransformConfig(targetId, null);
                String target = cb.getString("target");
                t.put("targetEntity", target);
                t.put("targetEntityLabel", EasyMetaFactory.getLabel(target));
            } catch (Exception ignored) {
                // 转换配置已删除，保留 actionContent 中的原始引用供分析
            }
        }
    }

    /**
     * 触发时机位掩码转名称与描述
     *
     * @param t
     * @param whenMask
     */
    private void buildWhen(JSONObject t, int whenMask) {
        if (whenMask == 0) {
            // 历史脏数据或更高版本的未知时机，不报错但需告知模型无法判定
            t.put("whenUnknown", true);
            return;
        }

        JSONArray whenNames = new JSONArray();
        List<String> whenLabels = new ArrayList<>();
        for (TriggerWhen tw : TriggerWhen.values()) {
            if ((whenMask & tw.getMaskValue()) != 0) {
                whenNames.add(tw.name());
                whenLabels.add(WHEN_LABELS.getOrDefault(tw, tw.name()));
            }
        }
        t.put("when", whenNames);
        t.put("whenLabel", StringUtils.join(whenLabels, "、"));
    }

    /**
     * @param t
     * @param actionType
     */
    private void buildActionType(JSONObject t, String actionType) {
        t.put("actionType", actionType);
        if (StringUtils.isBlank(actionType)) {
            t.put("actionTypeUnknown", true);
            return;
        }
        try {
            t.put("actionTypeLabel", Language.L(ActionType.valueOf(actionType)));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // 未知动作类型（可能来自更高版本）
            t.put("actionTypeUnknown", true);
        }
    }

    /**
     * @param t
     * @param templateType
     */
    private void buildTemplateType(JSONObject t, int templateType) {
        t.put("templateType", templateType);
        String label;
        if (templateType == DataReportManager.TYPE_LIST) label = "列表";
        else if (templateType == DataReportManager.TYPE_HTML5) label = "HTML5";
        else if (templateType == DataReportManager.TYPE_WORD) label = "Word";
        else if (templateType == DataReportManager.TYPE_RECORD) label = "记录";
        else label = null;

        if (label != null) t.put("templateTypeLabel", label);
        else t.put("templateTypeUnknown", true);
    }

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
