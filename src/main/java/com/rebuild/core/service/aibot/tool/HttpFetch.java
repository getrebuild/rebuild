/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 资源调用工具，支持 GET / POST 请求
 *
 * @author devezhao
 * @since 2026/7/10
 */
@Slf4j
public class HttpFetch implements Tool {

    private final static int MAX_LEN = 20000;

    private static final List<String> BLOCKED_HEADERS = Arrays.asList(
            "host", "connection", "content-length", "transfer-encoding",
            "authorization", "proxy-authorization", "cookie", "set-cookie",
            "expect", "te", "upgrade", "via");

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = JSON.parseObject(arguments);
        String url = args.getString("url");
        if (StringUtils.isBlank(url)) {
            throw new ToolException("URL cannot be blank");
        }

        String method = args.getString("method");
        if (StringUtils.isBlank(method)) method = "GET";
        method = method.toUpperCase();

        // Headers
        Map<String, String> headers = null;
        JSONObject headersJson = args.getJSONObject("headers");
        if (MapUtils.isNotEmpty(headersJson)) {
            headers = new HashMap<>();
            for (Map.Entry<String, Object> e : headersJson.entrySet()) {
                String hName = e.getKey();
                if (BLOCKED_HEADERS.contains(hName.toLowerCase())) {
                    log.warn("Blocked unsafe header : {}", hName);
                    continue;
                }
                headers.put(hName, e.getValue() == null ? "" : e.getValue().toString());
            }
        }

        String result;
        if ("POST".equals(method)) {
            String body = args.getString("body");
            Object postData;
            if (StringUtils.isNotBlank(body)) {
                try {
                    postData = JSON.parse(body);
                } catch (Exception ex) {
                    postData = body;
                }
            } else {
                postData = "";
            }

            result = OkHttpUtils.post(url, postData, headers);
        } else {
            result = OkHttpUtils.get(url, headers);
        }

        if (result.length() > MAX_LEN) {
            result = result.substring(0, MAX_LEN) + "\n\n... (truncated, total " + result.length() + " chars)";
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "response"},
                new Object[]{"ok", result});
    }
}
