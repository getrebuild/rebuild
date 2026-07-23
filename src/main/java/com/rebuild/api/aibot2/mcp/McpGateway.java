/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.aibot2.mcp;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    private static final RequestRateLimiter RRL = RateLimiters.createRateLimiter(
            new int[]{600, 6000, 36000},
            new int[]{30, 100, 500});

    private static final McpServer MCP_SERVER = new McpServer(
            () -> RebuildConfiguration.get(ConfigurationItem.AppName),
            () -> Application.VER);

    @GetMapping("sse")
    public SseEmitter mcpSse(HttpServletRequest request, HttpServletResponse response) {
        String ak = extractBearerToken(request);
        if (ak == null || verifyAk(ak) == null) {
            response.setStatus(401);
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @PostMapping("sse")
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
