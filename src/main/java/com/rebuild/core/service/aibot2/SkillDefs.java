/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.CommonsConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 技能管理。技能数据存储于 CommonsConfig（type=AIBOT_SKILL），
 * 每个 skill 的 config JSON 格式为 {"name":"技能名","prompt":"提示词","description":"描述"}
 *
 * @author Zixin
 * @since 2026/7/15
 */
@Slf4j
public class SkillDefs {

    /**
     * 列出所有可用技能（供前端选择器使用）
     *
     * @return
     */
    public static List<JSONObject> listSkills() {
        ConfigBean[] cbs = CommonsConfigManager.instance.getAibotSkills();
        List<JSONObject> skills = new ArrayList<>();
        for (ConfigBean cb : cbs) {
            if (Boolean.TRUE.equals(cb.getBoolean("isDisabled"))) continue;

            JSONObject config = (JSONObject) cb.getJSON("config");
            if (config == null) continue;

            JSONObject skill = new JSONObject();
            skill.put("id", cb.getID("id").toLiteral());
            skill.put("name", config.getString("name"));
            skill.put("description", config.getString("description"));
            skills.add(skill);
        }
        return skills;
    }

    /**
     * 获取技能的系统提示词。支持逗号分隔的多个技能名称
     *
     * @param skillName 单个或逗号分隔的多个技能名称
     * @return 合并后的提示词，无匹配则返回 null
     */
    public static String getSystemPrompt(String skillName) {
        if (StringUtils.isBlank(skillName)) return null;

        String[] names = skillName.split(",");
        StringBuilder prompts = new StringBuilder();

        ConfigBean[] cbs = CommonsConfigManager.instance.getAibotSkills();
        for (ConfigBean cb : cbs) {
            if (Boolean.TRUE.equals(cb.getBoolean("isDisabled"))) continue;

            JSONObject config = (JSONObject) cb.getJSON("config");
            if (config == null) continue;

            String name = config.getString("name");
            String prompt = config.getString("prompt");
            if (name == null || prompt == null) continue;

            for (String n : names) {
                if (name.equalsIgnoreCase(n.trim())) {
                    if (prompts.length() > 0) prompts.append("\n\n");
                    prompts.append(prompt);
                    log.info("Skill loaded: {}", name);
                    break;
                }
            }
        }

        return prompts.length() > 0 ? prompts.toString() : null;
    }
}
