/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Zixin
 * @since 2026/6/9
 */
@Slf4j
public class ToolDefs {

    private static final Map<String, Tool> TOOL_MAP = new HashMap<>();
    static {
        register(new EntitiesMeta());
        register(new HttpFetch());
        register(new SuggestCustom());
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
     * @return
     */
    public static List<ChatCompletionTool> tools() {
        return TOOL_MAP.values().stream()
                .map(Tool::def).collect(Collectors.toList());
    }

    /**
     * 列出所有可用工具（供前端展示）
     *
     * @return
     */
    public static List<JSONObject> listTools() {
        List<JSONObject> tools = new ArrayList<>();
        for (String toolName : TOOL_MAP.keySet()) {
            String d = CommonsUtils.getStringOfRes("aibot2/tool/" + toolName + ".json");
            if (d == null) continue;

            JSONObject json = JSONObject.parseObject(d);
            JSONObject funcJson = json.getJSONObject("function");

            JSONObject tool = new JSONObject();
            tool.put("name", funcJson.getString("name"));
            tool.put("description", funcJson.getString("description"));
            tools.add(tool);
        }
        return tools;
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
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"error", "Tool not found: " + toolName}).toJSONString();
        }

        log.info("Tool call: {} args={}", toolName, arguments);
        try {
            Object res = tool.tool(arguments);
            String toolRes = res instanceof String ? (String) res : JSON.toJSONString(res);
            log.info("Tool result: {}", toolRes);
            return toolRes;

        } catch (Exception ex) {
            log.error("Tool execution failed : {}", toolName, ex);

            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"error", ex.getMessage()}).toJSONString();
        }
    }
}
