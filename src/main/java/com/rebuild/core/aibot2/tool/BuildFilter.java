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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.configuration.general.AdvFilterService;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 新建高级过滤器（查询方案）。字段引用支持中文标签，自动转换为真实字段名
 *
 * @author devezhao
 * @since 2026/8/16
 */
@Slf4j
public class BuildFilter implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        // 1. 实体
        String entityIdent = args.getString("entity");
        if (StringUtils.isBlank(entityIdent)) {
            throw new KnownToolException("实体 (entity) 不能为空");
        }
        Entity entity = ToolHelper.resolveEntity(entityIdent);
        if (entity == null) {
            throw new KnownToolException("未知实体 : " + entityIdent + ToolHelper.suggestEntity(entityIdent));
        }

        String filterName = args.getString("filterName");
        if (StringUtils.isBlank(filterName)) {
            throw new KnownToolException("过滤器名称 (filterName) 不能为空");
        }

        JSONObject config = args.getJSONObject("config");
        if (config == null || config.isEmpty()) {
            throw new KnownToolException("过滤条件 (config) 不能为空，请先用 GetConfigSchema(schema=adv-filter) 获取其结构定义");
        }

        // 统一使用真实实体名
        config.put("entity", entity.getName());

        // 2. Schema 校验（错误明细回传，供自修复重试）
        List<String> schemaErrors = JsonSchemaValidator.validateErrors(JsonSchemaValidator.ADV_FILTER, config);
        if (schemaErrors != null && !schemaErrors.isEmpty()) {
            throw new KnownToolException("过滤条件不符合规范 : " + ToolHelper.joinErrors(schemaErrors)
                    + "。请修正后重试，可用 GetConfigSchema(schema=adv-filter) 查看完整定义");
        }

        // 3. 语义解析与校验：字段标签转真实字段名，并经 AdvFilterParser 完整解析验证
        resolveFilterItems(entity, config);
        try {
            new AdvFilterParser(config, entity).toSqlWhere();
        } catch (Exception ex) {
            throw new KnownToolException("过滤条件解析失败 : " + ex.getLocalizedMessage(), ex);
        }

        // 4. 共享范围。私有时归属操作人（归属 AI 助手将无人可见）
        boolean shareSelf = ShareToManager.SHARE_SELF.equalsIgnoreCase(args.getString("shareTo"));
        String shareTo = shareSelf ? ShareToManager.SHARE_SELF : ShareToManager.SHARE_ALL;
        ID editor = shareSelf ? UserContextHolder.getUser() : UserService.AIBOT_USER;

        // 5. 未确认时仅返回改动清单
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = new JSONObject(true);
            changes.put("操作", "新建过滤器");
            changes.put("所属实体", EasyMetaFactory.getLabel(entity));
            changes.put("过滤器名称", filterName);
            changes.put("共享范围", shareSelf ? "仅创建人可见" : "全部用户");
            changes.put("过滤条件", config);
            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        // 6. 落库
        Record record = EntityHelper.forNew(EntityHelper.FilterConfig, editor);
        record.setString("belongEntity", entity.getName());
        record.setString("filterName", filterName);
        record.setString("config", config.toJSONString());
        record.setString("shareTo", shareTo);
        record = Application.getBean(AdvFilterService.class).create(record);
        ID configId = record.getPrimary();

        log.info("AdvFilter created via AI : {} on {}", configId, entity.getName());

        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "entity", "filterName", "message"},
                new Object[]{"ok", configId.toLiteral(), entity.getName(), filterName,
                        String.format("已成功创建过滤器 [%s]（%s），用户打开该实体列表页后可在常用筛选中选择使用，请提醒用户核对筛选条件是否符合预期",
                                filterName, EasyMetaFactory.getLabel(entity))});
    }

    /**
     * 解析过滤条件项中的字段引用（标签转真实字段名）
     *
     * @param entity
     * @param config
     */
    private void resolveFilterItems(Entity entity, JSONObject config) {
        JSONArray items = config.getJSONArray("items");
        if (items == null) return;

        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) continue;

            String field = item.getString("field");
            if (StringUtils.isNotBlank(field)) {
                item.put("field", resolveFilterField(entity, field));
            }
        }
    }

    /**
     * 解析过滤字段（支持虚拟字段、& 引用名称字段、点号跨实体路径）
     *
     * @param entity
     * @param field
     * @return
     */
    private String resolveFilterField(Entity entity, String field) {
        // 虚拟字段，如 $CURRENTUSER$
        if (field.startsWith("$")) {
            return field;
        }

        // & 前缀：按引用实体的名称字段查询
        if (field.startsWith("&")) {
            String path = field.substring(1);
            if (path.startsWith("amp;")) path = path.substring(4);
            return "&" + ToolHelper.resolveFieldPath(entity, path);
        }

        return ToolHelper.resolveFieldPath(entity, field);
    }
}
