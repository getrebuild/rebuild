/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.aibot2.tool.ToolDefs;
import com.rebuild.core.aibot2.tool.UserMemory;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;

/**
 * 系统提示词构建。各层以标签包裹，避免指令混杂
 *
 * @author Zixin
 * @since 2026/8/12
 */
@Slf4j
public class SystemPromptBuilder {

    /**
     * 构建分层系统提示词
     *
     * @param basePrompt
     * @param agentPrompt
     * @param skillName
     * @return
     */
    public static String build(String basePrompt, String agentPrompt, String skillName) {
        StringBuilder systemPrompt = new StringBuilder();

        // 基础要求（管理中心配置）
        if (StringUtils.isNotBlank(basePrompt)) {
            systemPrompt.append("<basic_requirements>\n").append(basePrompt.trim()).append("\n</basic_requirements>");
        }

        // Agent 提示词（优先级高于基础要求）
        if (StringUtils.isNotBlank(agentPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<agent_prompt>\n")
                    .append("以下是当前 Agent 的专属提示词，与前述要求冲突时以本节为准。\n\n")
                    .append(agentPrompt.trim())
                    .append("\n</agent_prompt>");
        }

        // 系统能力（资源文件）
        String capabilityPrompt = Config.getSystemCapabilityPrompt();
        if (StringUtils.isNotBlank(capabilityPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<system_capabilities>\n").append(capabilityPrompt.trim()).append("\n</system_capabilities>");
        }

        // 会话上下文（系统信息 + 当前用户信息）
        ID user = UserContextHolder.getUser(true);
        boolean isRealUser = user != null && !UserHelper.isSystemUser(user);

        if (isRealUser) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<session_context>\n").append(buildSessionContext(user)).append("\n</session_context>");
        }

        // 用户记忆（工具启用时注入使用指引与记忆内容）
        if (isRealUser && !ToolDefs.isToolDisabled("UserMemory")) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<user_memory>\n").append(UserMemory.MEMORY_GUIDANCE);

            String memoryPrompt = buildMemoryPrompt(user);
            if (StringUtils.isNotBlank(memoryPrompt)) {
                systemPrompt.append("\n\n以下为当前用户的个性化记忆，仅供参考，不要主动向用户复述这些内容。\n").append(memoryPrompt);
            }
            systemPrompt.append("\n</user_memory>");
        }

        // 技能（用户指定，冲突时优先）
        String skillPrompt = SkillDefs.getSkillPrompt(skillName);
        if (skillPrompt != null) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("<current_skill>\n")
                    .append("以下是用户为本次会话指定的技能要求，与前述要求冲突时以本节为准。\n\n")
                    .append(skillPrompt.trim())
                    .append("\n</current_skill>");
        }

        return systemPrompt.toString();
    }

    /**
     * 构建会话上下文（系统信息 + 当前用户信息）
     *
     * @param userId
     * @return
     */
    private static String buildSessionContext(ID userId) {
        StringBuilder sb = new StringBuilder();

        // 系统信息
        String appName = RebuildConfiguration.get(ConfigurationItem.AppName);
        String homeUrl = RebuildConfiguration.get(ConfigurationItem.HomeURL);
        sb.append("系统名称: ").append(appName);
        sb.append("\n主页地址: ").append(homeUrl);
        sb.append("\n当前时间: ").append(CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));

        // 用户信息
        User u = Application.getUserStore().getUser(userId);
        sb.append("\n当前用户: ").append(u.getFullName());
        if (StringUtils.isNotBlank(u.getEmail())) sb.append("\n邮箱: ").append(u.getEmail());
        if (StringUtils.isNotBlank(u.getWorkphone())) sb.append("\n电话: ").append(u.getWorkphone());
        if (u.getOwningDept() != null) sb.append("\n部门: ").append(u.getOwningDept().getName());
        if (u.getOwningRole() != null) sb.append("\n角色: ").append(u.getOwningRole().getName());
        if (!u.getOwningTeams().isEmpty()) {
            StringBuilder teams = new StringBuilder();
            for (Team t : u.getOwningTeams()) {
                if (teams.length() > 0) teams.append("、");
                teams.append(t.getName());
            }
            if (teams.length() > 0) sb.append("\n团队: ").append(teams);
        }

        return sb.toString();
    }

    /**
     * 构建用户记忆注入文本
     *
     * @param user
     * @return
     */
    private static String buildMemoryPrompt(ID user) {
        ConfigBean[] memories = AibotConfigManager.instance.getUserMemoryConfigs(user).clone();
        if (memories.length == 0) return null;

        // level 升序（高档在前），同档按 createdOn 降序（新在前）
        Arrays.sort(memories, (a, b) -> {
            int c = Integer.compare(a.getInteger("level"), b.getInteger("level"));
            if (c != 0) return c;
            return ((Date) b.getObject("createdOn")).compareTo((Date) a.getObject("createdOn"));
        });

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < memories.length; i++) {
            sb.append("- ").append(memories[i].getString("content"));
            if (i < memories.length - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
