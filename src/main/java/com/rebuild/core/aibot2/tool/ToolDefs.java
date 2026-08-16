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
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.AdminGuard;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .filter(e -> !disabled.contains(e.getKey()) && !e.getValue().isSystem())
                .filter(e -> isAdmin || !(e.getValue() instanceof AdminGuard))
                .map(e -> e.getValue().def())
                .collect(Collectors.toList());
    }

    /**
     * 根据名称执行工具
     *
     * @param toolName
     * @param arguments
     * @return
     */
    public static String execute(String toolName, String arguments) {
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

        // 统一空值保护
        if (StringUtils.isBlank(arguments)) arguments = "{}";

        log.info("Tool call: {} args={}", toolName, arguments);
        try {
            Object res = tool.tool(arguments);
            String toolRes = res instanceof String ? (String) res : JSON.toJSONString(res);
            log.info("Tool result: {}", toolRes);
            return toolRes;

        } catch (KnownToolException ex) {
            // 已知业务异常（如参数校验失败、实体不存在），仅记录消息不输出堆栈
            log.warn("Tool execution blocked : {} - {}", toolName, ex.getMessage());
            throw ex;
        } catch (ToolException ex) {
            log.error("Tool execution failed : {}", toolName, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Tool execution failed : {}", toolName, ex);
            throw new ToolException(CommonsUtils.getRootMessage(ex), ex);
        }
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
     * 获取已禁用的工具名称集合
     *
     * @return
     */
    private static Set<String> getDisabledTools() {
        String value = RebuildConfiguration.get(ConfigurationItem.AibotToolsDisabled);
        if (StringUtils.isBlank(value)) return Collections.emptySet();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
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
            // 禁用工具仅在 includeDisabled 时返回（供管理页展示/重新启用）
            if (disabled.contains(toolName) && !includeDisabled) continue;

            String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
            if (d == null) continue;

            JSONObject funcJson = JSONObject.parseObject(d).getJSONObject("function");

            JSONObject tool = new JSONObject(true);
            tool.put("name", funcJson.getString("name"));
            tool.put("description", funcJson.getString("description"));
            if (includeDisabled) tool.put("disabled", disabled.contains(toolName));
            if (includeSchema) tool.put("inputSchema", funcJson.getJSONObject("parameters"));
            tools.add(tool);
        }
        return tools;
    }
}
