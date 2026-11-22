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
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

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
     * @param replyStyle
     * @param skillName
     * @param planMode
     * @param planConfirmed
     * @param agentTools
     * @return
     */
    public static String build(String basePrompt, String agentPrompt, String replyStyle, String skillName, boolean planMode, boolean planConfirmed, Set<String> agentTools) {
        StringBuilder systemPrompt = new StringBuilder();

        // 助手身份（Agent 会话有专属人设，不注入）
        if (StringUtils.isBlank(agentPrompt)) {
            systemPrompt.append("<assistant_identity>\n")
                    .append(Prompts.render("identity", "name", RebuildConfiguration.get(ConfigurationItem.AibotName)))
                    .append("\n</assistant_identity>");
        }

        // 基础要求 / Agent 提示词（二选一：指定 Agent 时用其提示词替代基础要求，未指定则使用基础要求）
        if (StringUtils.isNotBlank(agentPrompt)) {
            systemPrompt.append("<agent_prompt>\n").append(agentPrompt.trim()).append("\n</agent_prompt>");
        } else if (StringUtils.isNotBlank(basePrompt)) {
            systemPrompt.append("<basic_requirements>\n").append(basePrompt.trim()).append("\n</basic_requirements>");
        }

        // 回复风格（预设提示词片段，未设置则不注入）
        String stylePrompt = buildReplyStylePrompt(replyStyle);
        if (stylePrompt != null) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<reply_style>\n").append(stylePrompt).append("\n</reply_style>");
        }

        // 系统能力（资源文件 + 动态追加工具使用指引）
        String capabilityPrompt = Config.getSystemCapabilityPrompt();
        String searchGuidance = buildSearchToolGuidance(agentTools);
        if (StringUtils.isNotBlank(searchGuidance)) {
            capabilityPrompt = StringUtils.isBlank(capabilityPrompt)
                    ? searchGuidance : capabilityPrompt.trim() + NN + searchGuidance;
        }
        String entityGuidance = buildEntityFieldGuidance(agentTools);
        if (StringUtils.isNotBlank(entityGuidance)) {
            capabilityPrompt = StringUtils.isBlank(capabilityPrompt)
                    ? entityGuidance : capabilityPrompt.trim() + NN + entityGuidance;
        }
        if (StringUtils.isNotBlank(capabilityPrompt)) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<system_capabilities>\n").append(capabilityPrompt.trim()).append("\n</system_capabilities>");
        }

        // 会话上下文（系统信息 + 当前用户信息）
        ID user = UserContextHolder.getUser(true);
        boolean isRealUser = user != null && !UserHelper.isSystemUser(user) && !AibotContextHolder.isAnonymous();

        if (isRealUser) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<session_context>\n").append(buildSessionContext(user)).append("\n</session_context>");
        }

        // 用户记忆（工具启用时注入使用指引与记忆内容）
        if (isRealUser && !ToolDefs.isToolDisabled("UserMemory", agentTools)) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<user_memory>\n").append(Prompts.get("memory-guidance"));

            String memoryPrompt = buildMemoryPrompt(user);
            if (StringUtils.isNotBlank(memoryPrompt)) {
                systemPrompt.append(NN).append(Prompts.get("memory-intro"))
                        .append("\n").append(memoryPrompt);
            }
            systemPrompt.append("\n</user_memory>");
        }

        // 计划模式（在技能之前注入，优先级低于技能）
        if (planMode) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<plan_mode>\n")
                    .append(Prompts.get("plan-mode"))
                    .append("\n</plan_mode>");
        }

        // 计划确认（用户已在计划模式中确认方案，写操作工具可直接执行）
        if (planConfirmed) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<plan_confirmed>\n")
                    .append(Prompts.get("plan-confirmed"))
                    .append("\n</plan_confirmed>");
        }

        // 回复格式（靠后强化，技能仍在其后）
        if (systemPrompt.length() > 0) systemPrompt.append(NN);
        systemPrompt.append("<output_format>\n")
                .append(Prompts.get("output-format"))
                .append("\n</output_format>");

        // 技能（用户指定，冲突时优先）
        String skillPrompt = SkillDefs.getSkillPrompt(skillName);
        if (skillPrompt != null) {
            if (systemPrompt.length() > 0) systemPrompt.append(NN);
            systemPrompt.append("<current_skill>\n")
                    .append(Prompts.render("current-skill", "prompt", skillPrompt.trim()))
                    .append("\n</current_skill>");
        }

        return systemPrompt.toString();
    }

    /**
     * 构建历史被裁剪的告知片段（记忆轮数生效时由调用方追加到系统提示词末尾）
     *
     * @param rounds 保留的轮数
     * @return
     */
    public static String buildTruncatedHistoryPrompt(Long rounds) {
        return "<truncated_history>\n"
                + Prompts.render("truncated-history", "rounds", rounds)
                + "\n</truncated_history>";
    }

    /**
     * 回复风格预设映射
     *
     * @param style 风格标识，可为 null
     * @return 提示词片段，未匹配返回 null
     */
    private static String buildReplyStylePrompt(String style) {
        if (StringUtils.isBlank(style)) return null;

        String key = style.trim().toLowerCase();
        switch (key) {
            case "concise":
            case "detailed":
            case "professional":
            case "friendly":
                break;
            default:
                log.warn("Unknown AI reply style : {}", style);
                return null;
        }

        String prompt = Prompts.get("reply-style-" + key);
        return StringUtils.isBlank(prompt) ? null : prompt;
    }

    /**
     * 根据可用工具动态生成搜索工具选择规则
     *
     * @return
     */
    private static String buildSearchToolGuidance(Set<String> agentTools) {
        boolean knowledgeEnabled = !ToolDefs.isToolDisabled("SearchKnowledge", agentTools);
        boolean helpEnabled = !ToolDefs.isToolDisabled("SearchHelp", agentTools);

        if (!knowledgeEnabled && !helpEnabled) return null;

        if (knowledgeEnabled && helpEnabled) {
            return Prompts.get("search-guidance-both");
        } else if (knowledgeEnabled) {
            return Prompts.get("search-guidance-knowledge");
        } else {
            return Prompts.get("search-guidance-help");
        }
    }

    /**
     * 根据可用工具动态生成字段与实体引用规范
     *
     * @param agentTools
     * @return
     */
    private static String buildEntityFieldGuidance(Set<String> agentTools) {
        if (ToolDefs.isToolDisabled("ListEntities", agentTools)) return null;

        return Prompts.get("entity-field-guidance");
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
