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

import static com.rebuild.core.aibot2.vector.VectorData.NN;

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
     * @param planMode
     * @param planConfirmed
     * @return
     */
    public static String build(String basePrompt, String agentPrompt, String skillName, boolean planMode, boolean planConfirmed) {
        StringBuilder systemPrompt = new StringBuilder();

        // 基础要求（管理中心配置）
        if (StringUtils.isNotBlank(basePrompt)) {
            systemPrompt.append("<basic_requirements>\n").append(basePrompt.trim()).append("\n</basic_requirements>");
        }

        // Agent 提示词（优先级高于基础要求）
        if (StringUtils.isNotBlank(agentPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<agent_prompt>\n")
                    .append("以下是当前 Agent 的专属提示词，与前述要求冲突时以本节为准。\n\n")
                    .append(agentPrompt.trim())
                    .append("\n</agent_prompt>");
        }

        // 系统能力（资源文件 + 动态追加搜索工具选择规则）
        String capabilityPrompt = Config.getSystemCapabilityPrompt();
        String searchGuidance = buildSearchToolGuidance();
        if (StringUtils.isNotBlank(searchGuidance)) {
            capabilityPrompt = StringUtils.isBlank(capabilityPrompt)
                    ? searchGuidance : capabilityPrompt.trim() + NN + searchGuidance;
        }
        if (StringUtils.isNotBlank(capabilityPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<system_capabilities>\n").append(capabilityPrompt.trim()).append("\n</system_capabilities>");
        }

        // 会话上下文（系统信息 + 当前用户信息）
        ID user = UserContextHolder.getUser(true);
        boolean isRealUser = user != null && !UserHelper.isSystemUser(user);

        if (isRealUser) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<session_context>\n").append(buildSessionContext(user)).append("\n</session_context>");
        }

        // 用户记忆（工具启用时注入使用指引与记忆内容）
        if (isRealUser && !ToolDefs.isToolDisabled("UserMemory")) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<user_memory>\n").append(UserMemory.MEMORY_GUIDANCE);

            String memoryPrompt = buildMemoryPrompt(user);
            if (StringUtils.isNotBlank(memoryPrompt)) {
                systemPrompt.append("\n\n以下为当前用户的个性化记忆，仅供参考，不要主动向用户复述这些内容。\n").append(memoryPrompt);
            }
            systemPrompt.append("\n</user_memory>");
        }

        // 计划模式（在技能之前注入，优先级低于技能）
        if (planMode) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<plan_mode>\n")
                    .append("用户已启用计划模式。你可以使用查询类工具收集信息（如查看现有实体、搜索记录等），但禁止执行任何写操作（创建、修改、删除等）。\n")
                    .append("请基于收集到的信息，输出执行方案，包括：\n")
                    .append("1. 将要调用的工具及参数\n")
                    .append("2. 具体操作步骤\n")
                    .append("3. 预期结果\n")
                    .append("用户确认后会发送确认消息触发实际执行。")
                    .append("\n</plan_mode>");
        }

        // 计划确认（用户已在计划模式中确认方案，写操作工具可直接执行）
        if (planConfirmed) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<plan_confirmed>\n")
                    .append("用户已在计划模式中确认了执行方案。调用写操作类工具时，请直接设置 confirmed=true 参数执行，无需再次征求用户确认。\n")
                    .append("\n</plan_confirmed>");
        }

        // 技能（用户指定，冲突时优先）
        String skillPrompt = SkillDefs.getSkillPrompt(skillName);
        if (skillPrompt != null) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<current_skill>\n")
                    .append("以下是用户为本次会话指定的技能要求，与前述要求冲突时以本节为准。\n\n")
                    .append(skillPrompt.trim())
                    .append("\n</current_skill>");
        }

        return systemPrompt.toString();
    }

    /**
     * 根据可用工具动态生成搜索工具选择规则
     *
     * @return
     */
    private static String buildSearchToolGuidance() {
        boolean knowledgeEnabled = !ToolDefs.isToolDisabled("SearchKnowledge");
        boolean helpEnabled = !ToolDefs.isToolDisabled("SearchHelp");

        if (!knowledgeEnabled && !helpEnabled) return null;

        StringBuilder sb = new StringBuilder("## 搜索工具选择\n\n");

        if (knowledgeEnabled && helpEnabled) {
            sb.append("- 用户提问涉及查阅资料、文档、制度、规范、流程等内容时，优先使用 SearchKnowledge 搜索企业知识库\n");
            sb.append("- 仅当知识库未匹配到相关内容，或用户明确询问 REBUILD 系统自身功能用法时，才使用 SearchHelp 搜索官方帮助文档\n");
            sb.append("- 不要同时调用两个搜索工具");
        } else if (knowledgeEnabled) {
            sb.append("- 用户提问涉及查阅资料、文档、制度、规范、流程等内容时，使用 SearchKnowledge 搜索企业知识库");
        } else {
            sb.append("- 用户询问系统功能用法或操作指引时，使用 SearchHelp 搜索官方帮助文档");
        }

        return sb.toString();
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
