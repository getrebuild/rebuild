/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.AibotAgent;
import com.rebuild.core.aibot2.ChatLogger;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.service.query.QueryHelper;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.rebuild.core.aibot2.tool.ToolHelper.compactJson;

/**
 * @author Zixin
 * @since 2026/6/9
 */
@Slf4j
public class ToolDefs {

    private static final Map<String, JSONObject> TOOL_JSON_CACHE = new ConcurrentHashMap<>();

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

        register(new BuildSkill());
        register(new GetConfigSchema());
        register(new BuildFilter());
        register(new BuildEntity());
        register(new BuildField());
        register(new BuildTrigger());
        register(new ListTriggers());
        register(new BuildTransform());
        register(new BuildReportTemplate());
        register(new BuildFormLayout());
        register(new BuildListLayout());
        register(new BuildNavMenu());
        register(new BuildApp());
        register(new BuildFrontJsCode());
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
        return tools(null);
    }

    /**
     * 获取可用工具
     * 
     * @param agent
     * @return
     */
    public static List<ChatCompletionTool> tools(AibotAgent agent) {
        // 管理员专属工具不提供给非管理员
        boolean isAdmin = UserContextHolder.getUser(true) != null
                && UserHelper.isAdmin(UserContextHolder.getUser());

        Set<String> disabled = getDisabledTools();
        Set<String> agentTools = agent != null ? agent.getTools() : null;

        return TOOL_MAP.entrySet().stream()
                .filter(e -> !disabled.contains(e.getKey()))
                .filter(e -> isAdmin || !(e.getValue() instanceof AdminGuard))
                .filter(e -> agentTools == null || agentTools.contains(e.getKey()))
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
            if (toolImpl.isSystem()) continue;
            if (disabled.contains(toolName) && !includeDisabled) continue;

            JSONObject toolJson = getToolJson(toolName);
            if (toolJson == null) continue;
            JSONObject funcJson = toolJson.getJSONObject("function");

            JSONObject d = new JSONObject(true);
            d.put("name", funcJson.getString("name"));
            d.put("description", funcJson.getString("description"));
            d.put("userDescription", toolJson.getString("userDescription"));

            if (includeDisabled) d.put("disabled", disabled.contains(toolName));
            if (includeSchema) d.put("inputSchema", funcJson.getJSONObject("parameters"));
            tools.add(d);
        }
        return tools;
    }

    /**
     * 获取并缓存工具定义 JSON
     *
     * @param toolName
     * @return
     */
    static JSONObject getToolJson(String toolName) {
        if (!Application.devMode()) {
            JSONObject c = TOOL_JSON_CACHE.get(toolName);
            if (c != null) return JSON.parseObject(c.toJSONString());
        }

        String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
        return d == null ? null : JSONObject.parseObject(d);
    }

    // 工具执行进度提示的兜底文案
    private static final String TOOL_HINT_DEFAULT = "正在处理...";

    /**
     * 工具执行中的用户可见提示。取 userDescription 首句，
     * 系统工具与描述缺失时回退兜底文案，避免内部工具名外泄到用户对话。
     *
     * @param toolName
     * @return
     */
    public static String userHint(String toolName) {
        Tool tool = TOOL_MAP.get(toolName);
        if (tool == null || tool.isSystem()) return TOOL_HINT_DEFAULT;

        JSONObject json = getToolJson(toolName);
        String desc = json == null ? null : json.getString("userDescription");
        if (StringUtils.isBlank(desc)) return TOOL_HINT_DEFAULT;

        desc = StringUtils.substringBefore(desc, "。").trim();
        if (desc.length() > 24) desc = CommonsUtils.maxstr(desc, 24) + "...";
        return StringUtils.defaultIfBlank(desc, TOOL_HINT_DEFAULT);
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

            // 创建记录/配置时，查询实际存储的记录数据并记录到日志
            if (chatLogger != null) {
                try {
                    JSONObject resultJson = JSON.parseObject(toolRes);
                    if ("ok".equals(resultJson.getString("status"))) {
                        String recordIdStr = resultJson.getString("id");
                        if (ID.isId(recordIdStr)) {
                            Record record = QueryHelper.recordNoFilter(ID.valueOf(recordIdStr));
                            chatLogger.log("TOOL_RECORD " + toolName, JSON.toJSONString(record));
                        }
                    }
                } catch (Exception ignored) {
                    // 查询失败不影响正常流程
                }
            }
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
