/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.DefinedException;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.ChatLogger;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rebuild.core.aibot2.tool.ToolHelper.compactJson;

/**
 * @author Zixin
 * @since 2026/6/9
 */
@Slf4j
public class ToolDefs {

    private static final Map<String, Tool> TOOL_MAP = new LinkedHashMap<>();
    static {
        register(new ListEntities());
        register(new FetchUrl());
        register(new SuggestCustom());
        register(new SearchHelp());
        register(new UploadFile());
        register(new QueryRecords());
        register(new GetRecord());
        register(new StatisticsData());
        register(new UpsertRecord());
        register(new CreateFeed());
        register(new CreateProjectTask());
        register(new ExportReport());
        register(new ApproveRecord());
        register(new SearchKnowledge());
        register(new ScheduleTask());
        register(new UserMemory());
        register(new BuildEntity());
        register(new BuildField());
        register(new GetConfigSchema());
        register(new BuildTrigger());
        register(new BuildFilter());
        register(new BuildSkill());
    }

    /**
     * @param tool
     */
    public static void register(Tool tool) {
        String name = tool.getClass().getSimpleName();
        TOOL_MAP.put(name, tool);
        log.info("Tool registered : {}", name);
    }

    /**
     * 获取可用工具
     *
     * @return
     */
    public static List<ChatCompletionTool> tools() {
        Set<String> disabled = getDisabledTools();
        // 管理员专属工具不提供给非管理员
        boolean isAdmin = UserHelper.isAdmin(UserContextHolder.getUser());
        return TOOL_MAP.entrySet().stream()
                .filter(e -> !disabled.contains(e.getKey()))
                .filter(e -> isAdmin || !(e.getValue() instanceof AdminGuard))
                .map(e -> e.getValue().def())
                .collect(Collectors.toList());
    }

    /**
     * 判断异常链中是否包含系统已知业务异常（DefinedException 及其子类，或 ApprovalException）
     *
     * @param ex
     * @return
     */
    private static boolean isKnownBusinessException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof DefinedException || cause instanceof ApprovalException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 获取已禁用的工具名称集合
     *
     * @return
     */
    static Set<String> getDisabledTools() {
        Set<String> dTools = new HashSet<>();

        String value = RebuildConfiguration.get(ConfigurationItem.AibotToolsDisabled);
        if (StringUtils.isBlank(value)) return dTools;

        Set<String> d = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        dTools.addAll(d);
        return dTools;
    }

    /**
     * 列出工具定义
     *
     * @param includeDisabled
     * @param includeSchema
     * @return
     */
    public static List<JSONObject> listTools(boolean includeDisabled, boolean includeSchema) {
        Set<String> disabled = getDisabledTools();
        List<JSONObject> tools = new ArrayList<>();
        for (String toolName : TOOL_MAP.keySet()) {
            Tool toolImpl = TOOL_MAP.get(toolName);
            // 系统工具仅供 AI 使用，不对用户展示
            if (toolImpl.isSystem()) continue;
            // 禁用工具仅在 includeDisabled 时返回（供管理页展示/重新启用）
            if (disabled.contains(toolName) && !includeDisabled) continue;

            String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
            if (d == null) continue;

            JSONObject json = JSONObject.parseObject(d);
            JSONObject funcJson = json.getJSONObject("function");

            JSONObject tool = new JSONObject(true);
            tool.put("name", funcJson.getString("name"));
            tool.put("description", funcJson.getString("description"));
            // 用户描述独立于模型描述，未配置时回退到模型描述
            String userDescription = json.getString("userDescription");
            if (StringUtils.isNotBlank(userDescription)) tool.put("userDescription", userDescription);
            if (includeDisabled) tool.put("disabled", disabled.contains(toolName));
            if (includeSchema) tool.put("inputSchema", funcJson.getJSONObject("parameters"));
            tools.add(tool);
        }
        return tools;
    }

    /**
     * 工具是否被禁用
     *
     * @param toolName
     * @return
     */
    public static boolean isToolDisabled(String toolName) {
        return getDisabledTools().contains(toolName);
    }

    /**
     * 根据名称执行工具
     *
     * @param toolName
     * @param arguments
     * @return
     */
    public static String execute(String toolName, String arguments) {
        return execute(toolName, arguments, null);
    }

    /**
     * 根据名称执行工具，可附带 ChatLogger 记录会话日志
     *
     * @param toolName
     * @param arguments
     * @param chatLogger 可为 null
     * @return
     */
    public static String execute(String toolName, String arguments, ChatLogger chatLogger) {
        ID user = UserContextHolder.getUser();

        Tool tool = TOOL_MAP.get(toolName);
        if (tool == null) {
            log.warn("Tool not found : {}", toolName);
            throw new KnownToolException("Tool not found: " + toolName);
        }

        if (isToolDisabled(toolName)) {
            log.warn("Tool disabled : {}", toolName);
            throw new KnownToolException("Tool disabled: " + toolName);
        }

        // 管理员专属工具验证权限
        if (tool instanceof AdminGuard && !UserHelper.isAdmin(user)) {
            log.warn("Tool requires admin : {} by {}", toolName, user);
            throw new KnownToolException("此操作仅限管理员使用");
        }

        if (StringUtils.isBlank(arguments)) arguments = "{}";

        // TOOL_CALL 由 execute 统一记录，避免 executeSafely 重复打印
        log.info("TOOL_CALL {}\n{}", toolName, compactJson(arguments));
        if (chatLogger != null) chatLogger.log("TOOL_CALL " + toolName, arguments);

        try {
            Object res = tool.tool(arguments);
            String toolRes = res instanceof String ? (String) res : JSON.toJSONString(res);
            log.info("TOOL_RESULT {}\n{}", toolName, compactJson(toolRes));
            if (chatLogger != null) chatLogger.log("TOOL_RESULT " + toolName, toolRes);
            return toolRes;

        } catch (KnownToolException ex) {
            // 已知业务异常（如参数校验失败、实体不存在），仅记录消息不输出堆栈
            log.warn("TOOL_WARN {}\n{}", toolName, ex.getMessage());
            throw ex;
        } catch (ToolException ex) {
            log.error("TOOL_ERROR {}\n{}", toolName, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            String error = CommonsUtils.getRootMessage(ex);
            log.error("TOOL_ERROR {}\n{}", toolName, error, ex);
            throw new ToolException(error, ex);
        }
    }

    /**
     * 安全执行工具，异常时返回错误信息而非中断会话
     *
     * @param toolName
     * @param arguments
     * @param chatLogger
     * @return
     */
    public static String executeSafely(String toolName, String arguments, ChatLogger chatLogger) {
        String toolResult;
        try {
            toolResult = execute(toolName, arguments, chatLogger);
        } catch (Exception ex) {
            if (isKnownBusinessException(ex)) {
                String message = CommonsUtils.getRootMessage(ex);
                toolResult = "[业务校验错误] 此为系统已知的业务异常，请将以下错误信息如实反馈给用户，"
                        + "不要尝试修改数据或参数以绕过校验。\n错误信息: " + message;
            } else if (ex instanceof KnownToolException) {
                toolResult = ex.getMessage();
            } else {
                toolResult = CommonsUtils.getRootMessage(ex);
            }

            if (chatLogger != null) chatLogger.log("TOOL_RESULT " + toolName, toolResult);
        }
        return toolResult;
    }
}
