/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.aibot2.service.AibotConfigService;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 新建 AI 技能（仅管理员）
 *
 * @author devezhao
 * @since 2026/8/19
 */
@Slf4j
public class BuildSkill implements Tool, AdminGuard {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String name = args.getString("name");
        if (StringUtils.isBlank(name)) {
            throw new KnownToolException("技能名称 (name) 不能为空");
        }

        String prompt = args.getString("prompt");
        if (StringUtils.isBlank(prompt)) {
            throw new KnownToolException("技能提示词 (prompt) 不能为空");
        }

        String description = args.getString("description");

        // 未确认时返回改动清单
        if (!args.getBooleanValue("confirmed")) {
            JSONObject changes = new JSONObject(true);
            changes.put("操作", "新建技能");
            changes.put("技能名称", name);
            if (StringUtils.isNotBlank(description)) changes.put("描述", description);
            changes.put("提示词", prompt);

            // 同名技能检查
            for (ConfigBean cb : AibotConfigManager.instance.getSkillConfigs()) {
                JSONObject config = (JSONObject) cb.getJSON("config");
                if (config != null && name.equalsIgnoreCase(config.getString("name"))) {
                    changes.put("注意", "已存在同名技能「" + name + "」，创建后两个技能将共存");
                    break;
                }
            }

            return JSONUtils.toJSONObject(
                    new String[]{"status", "needConfirm", "changes", "message"},
                    new Object[]{"ok", true, changes,
                            "本次操作尚未执行。请先将改动清单完整转述给用户并征求确认，"
                                    + "用户明确同意后再以相同参数并设置 confirmed=true 重新调用本工具执行创建。"
                                    + "用户未确认或要求调整时不得执行创建"});
        }

        // 创建技能
        JSONObject config = new JSONObject(true);
        config.put("name", name);
        config.put("description", description);
        config.put("prompt", prompt);

        Record record = EntityHelper.forNew(EntityHelper.AibotConfig, UserContextHolder.getUser());
        record.setString("type", AibotConfigManager.TYPE_SKILL);
        record.setString("name", name);
        record.setString("config", config.toJSONString());
        record = Application.getBean(AibotConfigService.class).create(record);

        String skillId = record.getPrimary().toLiteral();
        String skillUrl = AppUtils.getContextPath("/admin/integration/aibot-kits");

        log.info("Skill created via AI : {} ({})", skillId, name);

        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "name", "url", "message"},
                new Object[]{"ok", skillId, name, skillUrl,
                        String.format("已成功创建技能 [%s]，[点击管理技能](%s)，请将此链接展示给用户，以便其核对提示词是否符合预期",
                                name, skillUrl)});
    }
}
