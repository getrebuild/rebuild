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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.datareport.DataReportConfigService;
import com.rebuild.core.service.datareport.DataReportManager;
import com.rebuild.core.service.datareport.TemplateExtractor33;
import com.rebuild.core.support.RbvFunction;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 新建网页报表模板（仅管理员，仅 HTML5 类型）。
 * 流程：参数校验 → 字段变量校验 → 用户确认 → 落库
 *
 * @author devezhao
 * @since 2026/8/19
 */
@Slf4j
public class BuildReportTemplate implements Tool, AdminGuard {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        // 实体
        String entityIdent = args.getString("entity");
        if (StringUtils.isBlank(entityIdent)) {
            throw new KnownToolException("实体 (entity) 不能为空");
        }
        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        // 报表名称
        String name = args.getString("name");
        if (StringUtils.isBlank(name)) {
            throw new KnownToolException("报表模板名称 (name) 不能为空");
        }

        // 模板内容
        String templateContent = args.getString("templateContent");
        if (StringUtils.isBlank(templateContent)) {
            throw new KnownToolException("模板内容 (templateContent) 不能为空");
        }

        // 校验明细语法
        boolean usesDetail = templateContent.contains("{detail}") || templateContent.contains("[detail$");
        if (usesDetail && entity.getDetailEntity() == null) {
            throw new KnownToolException("模板中使用了明细列表语法（{detail}），但实体 "
                    + EasyMetaFactory.getLabel(entity) + " 没有明细实体");
        }

        // 校验审批语法
        boolean usesApproval = templateContent.contains("{approval}") || templateContent.contains("[approval$");
        if (usesApproval && !MetadataHelper.hasApprovalField(entity)) {
            throw new KnownToolException("模板中使用了审批记录语法（{approval}），但实体 "
                    + EasyMetaFactory.getLabel(entity) + " 未配置审批流程");
        }

        // 校验模板变量是否真实存在（字段名必须来自 ListEntities 返回结果）
        Map<String, String> vars = RbvFunction.call().transformVarsHtml5(templateContent, entity.getName());
        if (vars != null) {
            Set<String> invalidVars = new HashSet<>();
            for (Map.Entry<String, String> e : vars.entrySet()) {
                if (e.getValue() == null && !TemplateExtractor33.isPlaceholder(e.getKey())) {
                    invalidVars.add(e.getKey());
                }
            }
            if (!invalidVars.isEmpty()) {
                throw new KnownToolException("模板中存在无效字段变量，请确认使用了 ListEntities 返回的真实字段名（区分大小写）。"
                        + "无效变量: " + String.join(", ", invalidVars));
            }
        }

        // 过滤条件
        JSONObject useFilter = args.getJSONObject("useFilter");
        if (useFilter != null && !useFilter.isEmpty()) {
            ToolHelper.validateFilter(entity, useFilter);
        }

        // 构建扩展配置
        JSONObject extraDef = new JSONObject(true);
        extraDef.put("outputType", "html5");
        extraDef.put("templateVersion", 3);
        if (useFilter != null && !useFilter.isEmpty()) {
            extraDef.put("useFilter", useFilter.toJSONString());
        }

        // 二次确认
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = new JSONObject(true);
            changes.put("操作", "新建网页报表模板");
            changes.put("所属实体", EasyMetaFactory.getLabel(entity));
            changes.put("模板名称", name);
            // 模板内容较长，截取摘要展示
            String preview = templateContent.length() > 200
                    ? templateContent.substring(0, 200) + "..." : templateContent;
            changes.put("模板内容（摘要）", preview);
            if (useFilter != null && !useFilter.isEmpty()) {
                changes.put("过滤条件", useFilter);
            }
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        // 落库
        Record record = EntityHelper.forNew(EntityHelper.DataReportConfig, UserService.AIBOT_USER);
        record.setString("belongEntity", entity.getName());
        record.setString("name", name);
        record.setString("templateContent", templateContent);
        record.setInt("templateType", DataReportManager.TYPE_HTML5);
        record.setString("extraDefinition", extraDef.toJSONString());
        record = Application.getBean(DataReportConfigService.class).create(record);
        ID configId = record.getPrimary();

        log.info("ReportTemplate(HTML5) created via AI : {} on {}", configId, entity.getName());

        String listUrl = AppUtils.getContextPath("/admin/data/report-templates");
        String designUrl = AppUtils.getContextPath("/admin/data/report-template/design?id=" + configId);
        String message = String.format("已成功创建网页报表模板 [%s]（%s），[点击进入模板设计器](%s) 编辑模板内容，或在 [报表模板列表](%s) 中查看，请将设计器链接展示给用户，以便其预览和调整模板",
                name, EasyMetaFactory.getLabel(entity), designUrl, listUrl);

        return JSONUtils.toJSONObject(
                new String[]{"status", "configId", "entity", "name", "url", "designUrl", "message"},
                new Object[]{"ok", configId.toLiteral(), entity.getName(), name, listUrl, designUrl, message});
    }
}
