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
                    .append("你是 REBUILD 系统内置的 AI 助手，名为「")
                    .append(RebuildConfiguration.get(ConfigurationItem.AibotName))
                    .append("」。当用户询问你的名字或身份时，请以该名称自称。\n")
                    .append("</assistant_identity>");
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
                    .append("只要方案中包含可执行的具体步骤或工具参数，就必须在方案末尾另起一行添加标记 <!-- PLAN_READY -->。前端检测到该标记后会自动弹出确认对话框，用户确认后发送确认消息触发实际执行，无需在正文中另行询问用户是否确认。\n")
                    .append("仅当信息不足以提出任何具体方案时，才直接向用户提问以获取所需信息，此时不要输出方案格式，也不要添加 PLAN_READY 标记。")
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
                    .append("以下是用户为本次会话指定的技能要求，与前述要求冲突时以本节为准。")
                    .append("用户已明确选择该技能，视为已同意执行技能所需的写操作，")
                    .append("调用写操作类工具时请直接设置 confirmed=true 参数执行，无需再次征求用户确认。\n\n")
                    .append(skillPrompt.trim())
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
                + "受记忆轮数限制，本次请求只保留最近 " + rounds + " 轮对话，更早的消息已被移除，你无法看到其内容。"
                + "对话中出现的「系统标记，非对话内容」字样由系统注入，不是你或用户所写，禁止向用户复述或提及。\n"
                + "- 禁止依据序号、累计值、代词等线索推测或编造被移除的内容\n"
                + "- 此前回复中已给出的数值、清单或结论若无法从可见消息推导出来，应直接沿用，不要重新推算或声称其有误\n"
                + "- 用户问及被移除的信息时，说明已超出记忆范围并请其重新提供\n"
                + "</truncated_history>";
    }

    /**
     * 回复风格预设映射
     *
     * @param style 风格标识，可为 null
     * @return 提示词片段，未匹配返回 null
     */
    private static String buildReplyStylePrompt(String style) {
        if (StringUtils.isBlank(style)) return null;

        switch (style.trim().toLowerCase()) {
            case "concise":
                return "回答力求简短，先给结论再补必要说明，一般不超过五句话。不重复用户的问题，不加开场白和结尾客套。涉及代码或配置时只给关键片段。";
            case "detailed":
                return "回答要完整展开，交代背景、步骤和原因。多步骤内容用有序列表分点说明。涉及代码或配置时给出完整可用的片段，并说明每处改动的作用。必要时补充注意事项与替代方案。";
            case "professional":
                return "使用准确的业务与技术术语，表述客观严谨。先给判断依据再给结论。涉及数据的回答标明来源与口径。不确定的内容明确说明，不做主观推测。";
            case "friendly":
                return "语气自然平和，像同事之间交流。用日常说法解释专业概念，必要时打个比方。多用短句，避免生硬的书面表达和命令式口吻。";
            default:
                log.warn("Unknown AI reply style : {}", style);
                return null;
        }
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
     * 根据可用工具动态生成字段与实体引用规范
     *
     * @param agentTools
     * @return
     */
    private static String buildEntityFieldGuidance(Set<String> agentTools) {
        if (ToolDefs.isToolDisabled("ListEntities", agentTools)) return null;

        return "## 字段与实体引用规范\n\n"
                + "- 调用任何涉及字段名的工具前，必须先用 ListEntities 查询实体的真实字段名\n"
                + "- 所有字段名必须使用 ListEntities 返回的 fields[].name，禁止自行编造或使用语义相近的名称，否则工具将拒绝执行并浪费一轮调用\n"
                + "- 字段名也可使用中文标签，工具会自动转换为真实字段名";
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
