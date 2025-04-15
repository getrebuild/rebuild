/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author devezhao
 * @since 2025/4/12
 */
@Slf4j
public class ChatClient {

    public static final ChatClient instance = new ChatClient();

    protected ChatClient() {}

    /**
     * @param chatid
     * @param user
     * @return
     */
    public Message post(String chatid, String user) {
        final String dsUrl = Config.getServerUrl("chat/completions");
        final String dsSecret = Config.getSecret();

        MessageCompletions completions = (MessageCompletions) Application.getCommonsCache().getx(chatid);
        completions.addMessage(user, "user");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + dsSecret);

        JSONObject resJson;
        try {
            String res = OkHttpUtils.post(dsUrl, completions.toCompletions(false), headers);
            resJson = JSON.parseObject(res);
            if (Application.devMode()) System.out.println("[dev] \n" + JSONUtils.prettyPrint(resJson));
        } catch (IOException ex) {
            throw new AiBotException(null, ex);
        }

        // choices[{message:xxx}]
        JSONObject choiceMessage = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
        Message resMessage = completions.addMessage(choiceMessage.getString("content"), choiceMessage.getString("role"));
        Application.getCommonsCache().putx(completions.getId(), completions);
        return resMessage;
    }

    /**
     * @param chatid
     * @param user
     * @param httpResp
     * @throws IOException
     */
    public void stream(String chatid, String user, HttpServletResponse httpResp) throws IOException {
        final String dsUrl = Config.getServerUrl("chat/completions");
        final String dsSecret = Config.getSecret();

        MessageCompletions completions = (MessageCompletions) Application.getCommonsCache().getx(chatid);
        completions.addMessage(user, "user");

        String reqBody = completions.toCompletions(true).toJSONString();
        RequestBody body = RequestBody.create(
                reqBody,
                MediaType.parse("application/json; charset=utf-8"));
        Request apiReq = new Request.Builder()
                .url(dsUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + dsSecret)
                .build();

        httpResp.setContentType(org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE);
        httpResp.setCharacterEncoding("UTF-8");
        httpResp.setHeader("Cache-Control", "no-cache");
        httpResp.setHeader("Connection", "keep-alive");

        StringBuilder deltaContent = new StringBuilder();

        // 执行请求并处理响应
        try (Response apiResp = OkHttpUtils.getHttpClient().newCall(apiReq).execute()) {
            try (BufferedSource source = Objects.requireNonNull(apiResp.body()).source();
                 PrintWriter writer = httpResp.getWriter()) {
                // [ERROR]
                if (!apiResp.isSuccessful()) {
                    StreamEcho.error("请求接口错误:" + apiResp.code(), writer);
                    return;
                }

                while (!source.exhausted()) {
                    String d = source.readUtf8Line();
                    if (d != null && d.startsWith("data: ")) {
                        d = d.substring(6);
                        if ("[DONE]".equals(d)) break;

                        JSONObject dJson = JSON.parseObject(d);
                        // choices[{delta:{content:xxx}}]
                        String chunk = dJson.getJSONArray("choices").getJSONObject(0).getJSONObject("delta").getString("content");
                        if (chunk.isEmpty()) continue;

                        StreamEcho.text(chunk, writer);
                        deltaContent.append(chunk);
                    }
                }

                // [DONE]
                completions.addMessage(deltaContent.toString(), "assistant");
                Application.getCommonsCache().putx(completions.getId(), completions);
            }
        }
    }

    /**
     * @param prompt
     * @return
     */
    public MessageCompletions createMessageCompletions(String prompt) {
        MessageCompletions c = new MessageCompletions(prompt);
        Application.getCommonsCache().putx(c.getId(), c);
        return c;
    }
}
