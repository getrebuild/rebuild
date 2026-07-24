/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
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
        register(new CreateFeed());
        register(new CreateProjectTask());
        register(new UploadFile());
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
        return TOOL_MAP.entrySet().stream()
                .filter(e -> !disabled.contains(e.getKey()))
                .map(e -> e.getValue().def())
                .collect(Collectors.toList());
    }

    /**
     * 根据名称执行工具
     *
     * @param toolName
     * @param arguments JSON 字符串
     * @return 执行结果（JSON 字符串）
     */
    public static String execute(String toolName, String arguments) {
        Tool tool = TOOL_MAP.get(toolName);
        if (tool == null) {
            log.warn("Tool not found : {}", toolName);
            throw new ToolException("Tool not found: " + toolName);
        }

        if (getDisabledTools().contains(toolName)) {
            log.warn("Tool disabled : {}", toolName);
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"error", "Tool disabled: " + toolName}).toJSONString();
        }

        log.info("Tool call: {} args={}", toolName, arguments);
        try {
            Object res = tool.tool(arguments);
            String toolRes = res instanceof String ? (String) res : JSON.toJSONString(res);
            log.info("Tool result: {}", toolRes);
            return toolRes;

        } catch (Exception ex) {
            log.error("Tool execution failed : {}", toolName, ex);
            throw new ToolException(CommonsUtils.getRootMessage(ex), ex);
        }
    }

    /**
     * 获取已禁用的工具名称集合（AibotToolsDisabled 配置，多个逗号分隔）
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
     * 列出所有可用工具（供前端展示）
     *
     * @return
     */
    public static List<JSONObject> listTools() {
        Set<String> disabled = getDisabledTools();
        List<JSONObject> tools = new ArrayList<>();
        for (String toolName : TOOL_MAP.keySet()) {
            String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
            if (d == null) continue;

            JSONObject dJson = JSONObject.parseObject(d);
            JSONObject funcJson = dJson.getJSONObject("function");

            JSONObject tool = new JSONObject();
            tool.put("name", funcJson.getString("name"));
            tool.put("description", funcJson.getString("description"));
            tool.put("disabled", disabled.contains(toolName));
            tools.add(tool);
        }
        return tools;
    }

    /**
     * 列出 MCP 协议格式的工具（含 inputSchema，供原生 MCP 端点 tools/list 使用）
     *
     * @return
     */
    public static List<JSONObject> mcpTools() {
        Set<String> disabled = getDisabledTools();
        List<JSONObject> tools = new ArrayList<>();
        for (String toolName : TOOL_MAP.keySet()) {
            if (disabled.contains(toolName)) continue;

            String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
            if (d == null) continue;

            JSONObject funcJson = JSONObject.parseObject(d).getJSONObject("function");

            JSONObject tool = new JSONObject(true);
            tool.put("name", funcJson.getString("name"));
            tool.put("description", funcJson.getString("description"));
            tool.put("inputSchema", funcJson.getJSONObject("parameters"));
            tools.add(tool);
        }
        return tools;
    }
}
