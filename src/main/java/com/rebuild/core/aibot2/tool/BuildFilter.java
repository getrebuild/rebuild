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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.JsonSchemaValidator;
import com.rebuild.core.configuration.general.AdvFilterService;
import com.rebuild.core.configuration.general.ShareToManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 新建高级过滤器
 *
 * @author devezhao
 * @since 2026/8/16
 */
@Slf4j
public class BuildFilter implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

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

        List<String> schemaErrors = JsonSchemaValidator.validateErrors(JsonSchemaValidator.ADV_FILTER, config);
        if (schemaErrors != null && !schemaErrors.isEmpty()) {
            throw new KnownToolException("过滤条件不符合规范 : " + ToolHelper.joinErrors(schemaErrors)
                    + "。请修正后重试，可用 GetConfigSchema(schema=adv-filter) 查看完整定义");
        }

        ToolHelper.validateFilter(entity, config);

        boolean shareSelf = ShareToManager.SHARE_SELF.equalsIgnoreCase(args.getString("shareTo"));
        String shareTo = shareSelf ? ShareToManager.SHARE_SELF : ShareToManager.SHARE_ALL;
        ID editor = shareSelf ? UserContextHolder.getUser() : UserService.AIBOT_USER;

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
}
