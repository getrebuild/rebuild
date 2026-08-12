/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import org.apache.commons.lang3.StringUtils;

/**
 * 系统提示词构建。各层以标签包裹，避免指令混杂
 *
 * @author Zixin
 * @since 2026/8/12
 */
public class SystemPromptBuilder {

    /**
     * 构建分层系统提示词
     *
     * @param basePrompt
     * @param skillName
     * @return
     */
    public static String build(String basePrompt, String skillName) {
        StringBuilder systemPrompt = new StringBuilder();

        // 基础要求（管理中心配置）
        if (StringUtils.isNotBlank(basePrompt)) {
            systemPrompt.append("<basic_requirements>\n").append(basePrompt.trim()).append("\n</basic_requirements>");
        }

        // 系统能力（资源文件）
        String capabilityPrompt = Config.getSystemCapabilityPrompt();
        if (StringUtils.isNotBlank(capabilityPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<system_capabilities>\n").append(capabilityPrompt.trim()).append("\n</system_capabilities>");
        }

        // 技能（用户指定，冲突时优先）
        String skillPrompt = SkillDefs.getSystemPrompt(skillName);
        if (skillPrompt != null) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<current_skill>\n")
                    .append("以下是用户为本次会话指定的技能要求，与前述要求冲突时以本节为准。\n\n")
                    .append(skillPrompt.trim())
                    .append("\n</current_skill>");
        }

        return systemPrompt.toString();
    }
}
