/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.aibot2.mcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.aibot.tool.ToolDefs;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

/**
 * MCP 协议服务器（Streamable HTTP / JSON-RPC 2.0）。
 * <p>
 * 传输无关：输入请求体字符串，输出响应体字符串（纯通知返回 null），
 * 由调用方决定以 JSON 或 SSE 等形式写出。工具直接取自 {@link ToolDefs}。
 *
 * @author REBUILD
 * @since 4.5
 */
@Slf4j
public class McpServer {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    // JSON-RPC 2.0 错误码
    public static final int ERR_PARSE = -32700;
    public static final int ERR_INVALID_REQUEST = -32600;
    public static final int ERR_METHOD_NOT_FOUND = -32601;
    public static final int ERR_INVALID_PARAMS = -32602;
    public static final int ERR_RATE_LIMIT = -32000;
    public static final int ERR_UNAUTHORIZED = -32001;

    private final Supplier<String> serverName;
    private final Supplier<String> serverVersion;

    /**
     * @param serverName 系统名称（动态）
     * @param serverVersion 系统版本（动态）
     */
    public McpServer(Supplier<String> serverName, Supplier<String> serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    /**
     * 处理 JSON-RPC 请求体（支持单条或批量）
     *
     * @param requestBody 原始请求体
     * @return 响应体字符串；纯通知（无需响应）返回 null
     */
    public String handle(String requestBody) {
        Object parsed;
        try {
            parsed = JSON.parse(requestBody);
        } catch (Exception e) {
            return errorBody(ERR_PARSE, "Parse error");
        }

        if (parsed instanceof JSONArray) {
            JSONArray batch = (JSONArray) parsed;
            JSONArray results = new JSONArray();
            for (Object item : batch) {
                if (!(item instanceof JSONObject)) {
                    results.add(rpcError(null, ERR_INVALID_REQUEST, "Invalid Request"));
                    continue;
                }
                JSONObject r = dispatch((JSONObject) item);
                if (r != null) results.add(r);
            }
            return results.isEmpty() ? null : results.toJSONString();

        } else if (parsed instanceof JSONObject) {
            JSONObject r = dispatch((JSONObject) parsed);
            return r == null ? null : r.toJSONString();

        } else {
            return errorBody(ERR_INVALID_REQUEST, "Invalid Request");
        }
    }

    /**
     * 构建 JSON-RPC 错误响应体（供传输层在认证/限流等阶段使用）
     *
     * @param code
     * @param message
     * @return
     */
    public static String errorBody(int code, String message) {
        JSONObject o = new JSONObject(true);
        o.put("jsonrpc", "2.0");
        o.put("id", null);
        JSONObject err = new JSONObject(true);
        err.put("code", code);
        err.put("message", message);
        o.put("error", err);
        return o.toJSONString();
    }

    private JSONObject dispatch(JSONObject req) {
        if (req == null) return null;

        Object id = req.get("id");
        String method = req.getString("method");
        JSONObject params = req.getJSONObject("params");
        if (params == null) params = new JSONObject();

        if ("initialize".equals(method)) return rpcResult(id, initResult(params));
        if ("ping".equals(method)) return rpcResult(id, new JSONObject());
        if ("tools/list".equals(method)) return rpcResult(id, toolsListResult());
        if ("tools/call".equals(method)) {
            String toolName = params.getString("name");
            if (StringUtils.isBlank(toolName)) {
                return rpcError(id, ERR_INVALID_PARAMS, "Invalid params: missing tool name");
            }
            return rpcResult(id, toolsCallResult(params));
        }

        // 通知（无 id，如 notifications/initialized）无需响应
        if (id == null) return null;
        return rpcError(id, ERR_METHOD_NOT_FOUND, "Method not found: " + method);
    }

    private JSONObject initResult(JSONObject params) {
        JSONObject result = new JSONObject(true);
        result.put("protocolVersion", PROTOCOL_VERSION);

        JSONObject caps = new JSONObject();
        caps.put("tools", new JSONObject());
        result.put("capabilities", caps);

        JSONObject info = new JSONObject(true);
        info.put("name", serverName.get());
        info.put("version", serverVersion.get());
        result.put("serverInfo", info);
        return result;
    }

    private JSONObject toolsListResult() {
        JSONObject result = new JSONObject(true);
        result.put("tools", ToolDefs.mcpTools());
        return result;
    }

    private JSONObject toolsCallResult(JSONObject params) {
        String name = params.getString("name");
        JSONObject args = params.getJSONObject("arguments");
        if (args == null) args = new JSONObject();

        JSONObject result = new JSONObject(true);
        JSONArray content = new JSONArray();
        JSONObject text = new JSONObject(true);
        text.put("type", "text");
        try {
            text.put("text", ToolDefs.execute(name, args.toJSONString()));
            result.put("isError", false);
        } catch (Exception ex) {
            log.error("MCP tools/call error : {}", name, ex);
            text.put("text", CommonsUtils.getRootMessage(ex));
            result.put("isError", true);
        }
        content.add(text);
        result.put("content", content);
        return result;
    }

    private JSONObject rpcResult(Object id, JSONObject result) {
        JSONObject o = new JSONObject(true);
        o.put("jsonrpc", "2.0");
        o.put("id", id);
        o.put("result", result);
        return o;
    }

    private JSONObject rpcError(Object id, int code, String message) {
        JSONObject o = new JSONObject(true);
        o.put("jsonrpc", "2.0");
        o.put("id", id);
        JSONObject err = new JSONObject(true);
        err.put("code", code);
        err.put("message", message);
        o.put("error", err);
        return o;
    }
}
