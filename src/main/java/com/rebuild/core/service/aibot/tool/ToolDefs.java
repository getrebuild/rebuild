/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.rebuild.core.RebuildException;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.ReflectUtils;

import java.util.HashMap;
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

    private static final Map<String, Tool> TOOL_MAP = new HashMap<>();

    static {
        autoScan();
    }

    /**
     * 自动扫描 com.rebuild 包下所有 Tool 实现类并注册
     */
    private static void autoScan() {
        Set<Class<?>> toolClasses;
        try {
            toolClasses = cn.devezhao.commons.ReflectUtils.getAllSubclasses(
                    Tool.class.getPackage().getName(), Tool.class);
            toolClasses.addAll(cn.devezhao.commons.ReflectUtils.getAllSubclasses(
                    "com.rebuild.rbv.aibot", Tool.class));
        } catch (Exception e) {
            throw new RebuildException("Failed to scan Tool subclasses", e);
        }

        for (Class<?> c : toolClasses) {
            Tool tool = (Tool) ReflectUtils.newInstance(c);
            register(tool);
        }

        log.info("Added {} Tool(s)", TOOL_MAP.size());
    }

    /**
     * @param tool
     */
    private static void register(Tool tool) {
        String name = tool.getClass().getSimpleName();
        TOOL_MAP.put(name, tool);
    }

    /**
     * @return
     */
    public static List<ChatCompletionTool> tools() {
        return TOOL_MAP.values().stream()
                .map(Tool::def).collect(Collectors.toList());
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
