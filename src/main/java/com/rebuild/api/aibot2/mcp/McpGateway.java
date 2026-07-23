/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.aibot2.mcp;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.service.aibot.tool.ToolDefs;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.rebuild.web.user.UserSettingsController.AK_PREFIX;
import static com.rebuild.web.user.UserSettingsController.KEY_REV;

/**
 * MCP Gateway
 * 认证方式：Authorization: Bearer <个人AK>
 *
 * @author REBUILD
 * @since 4.5
 */
@Slf4j
@RestController
@RequestMapping("/gw/mcp/")
public class McpGateway {

    // 基于用户限流
    private static final RequestRateLimiter RRL = RateLimiters.createRateLimiter(
            new int[]{60, 600, 3600},
            new int[]{30, 100, 500});

    // 原生 MCP 协议服务器：系统名称取 AppName，版本取 Application.VER，工具取自 ToolDefs
    private static final McpServer MCP_SERVER = new McpServer(
            () -> RebuildConfiguration.get(ConfigurationItem.AppName),
            () -> Application.VER);

    @PostMapping("tool/invoke")
    public JSON invokeTool(HttpServletRequest request) {
        String ak = extractBearerToken(request);
        if (ak == null) {
            return RespBody.error("Missing Authorization header", 401).toJSON();
        }
        ID user = verifyAk(ak);
        if (user == null) {
            return RespBody.error("Invalid Access Key", 401).toJSON();
        }

        if (RRL.overLimitWhenIncremented("mcp:" + user)) {
            return RespBody.error("Rate limit exceeded", 429).toJSON();
        }

        JSONObject body;
        try {
            body = (JSONObject) ServletUtils.getRequestJson(request);
        } catch (Exception e) {
            return RespBody.error("Invalid request body", 400).toJSON();
        }

        String tool = body.getString("tool");
        if (StringUtils.isBlank(tool)) {
            return RespBody.error("Missing [tool] parameter", 400).toJSON();
        }

        JSONObject params = body.getJSONObject("params");
        if (params == null) params = new JSONObject();

        try {
            UserContextHolder.setUser(user);
            String result = ToolDefs.execute(tool, params.toJSONString());
            return RespBody.ok(JSON.parse(result)).toJSON();
        } catch (Exception e) {
            log.error("MCP tool invoke error : {}", tool, e);
            return RespBody.error(e.getMessage(), 500).toJSON();
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * MCP Streamable HTTP 端点。
     * 用户在 AI 客户端直接配置本端点 URL 即可接入，无需本地代理。
     * 协议处理委托给可复用的 {@link McpServer}，此处仅负责认证、限流与传输。
     */
    @PostMapping
    public void mcp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ak = extractBearerToken(request);
        if (ak == null) {
            writeJson(response, 401, McpServer.errorBody(-32001, "Missing Authorization header"));
            return;
        }
        ID user = verifyAk(ak);
        if (user == null) {
            writeJson(response, 401, McpServer.errorBody(-32001, "Invalid Access Key"));
            return;
        }
        if (RRL.overLimitWhenIncremented("mcp:" + user)) {
            writeJson(response, 429, McpServer.errorBody(-32000, "Rate limit exceeded"));
            return;
        }

        String body;
        try {
            body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            writeJson(response, 400, McpServer.errorBody(-32700, "Parse error"));
            return;
        }

        UserContextHolder.setUser(user);
        try {
            String result = MCP_SERVER.handle(body);
            if (result == null) {
                response.setStatus(202);
                return;
            }
            writeJson(response, 200, result);
        } finally {
            UserContextHolder.clear();
        }
    }

    private ID verifyAk(String ak) {
        if (StringUtils.isBlank(ak) || !ak.startsWith(AK_PREFIX)) return null;
        String userId = KVStorage.getCustomValue(KEY_REV + EncryptUtils.toSHA256Hex(ak));
        return ID.isId(userId) ? ID.valueOf(userId) : null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
