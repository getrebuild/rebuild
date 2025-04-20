/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.utils.CommonsUtils;
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
     * @param chatRequest
     * @return
     */
    public Message post(ChatRequest chatRequest) {
        final String dsUrl = Config.getServerUrl("chat/completions");
        final String dsSecret = Config.getSecret();

        MessageCompletions completions = getOrNewMessageCompletions(chatRequest, Config.getBasePrompt());
        completions.addUserMessage(chatRequest);

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

        ChatStore.instance.store(completions);
        return resMessage;
    }

    /**
     * @param chatRequest
     * @param httpResp
     * @throws IOException
     */
    public void stream(ChatRequest chatRequest, HttpServletResponse httpResp) throws IOException {
        final String dsUrl = Config.getServerUrl("chat/completions");
        final String dsSecret = Config.getSecret();

        MessageCompletions completions = getOrNewMessageCompletions(chatRequest, Config.getBasePrompt());
        completions.addUserMessage(chatRequest);

        String reqBody = completions.toCompletions(true, chatRequest.getModel()).toJSONString();
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
        StringBuilder reasoningContent = new StringBuilder();

        // 执行请求并处理响应
        try (Response apiResp = OkHttpUtils.getHttpClient().newCall(apiReq).execute()) {
            try (BufferedSource source = Objects.requireNonNull(apiResp.body()).source();
                 PrintWriter writer = httpResp.getWriter()) {
                // [ERROR]
                if (!apiResp.isSuccessful()) {
                    StreamEcho.error("请求接口错误:" + apiResp.code(), writer);
                    return;
                }

                // 推理 0=未开始 1=输出中 2=结束
                int reasoningState = 0;
                while (!source.exhausted()) {
                    String d = source.readUtf8Line();
                    if (d != null && d.startsWith("data: ")) {
                        d = d.substring(6);
                        if ("[DONE]".equals(d)) {
                            StreamEcho.echo(completions.getChatid().toLiteral(), writer, "_chatid");
                            break;
                        }

                        JSONObject dJson = JSON.parseObject(d);
                        // choices[{delta:{content:xxx}}]
                        JSONObject delta = dJson.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
                        String chunk = delta.getString("content");
                        // 推理
                        if (chunk == null) {
                            String reasoning = delta.getString("reasoning_content");
                            if (reasoningState == 0) {
                                reasoningState = 1;  // 开始思考
                            }
                            chunk = reasoning;
                        } else if (reasoningState == 1) {
                            reasoningState = 2;  // 完成思考
                            StreamEcho.text("\n\n", writer);
                        }

                        if (chunk == null || chunk.isEmpty()) continue;

                        if (reasoningState == 1) {
                            StreamEcho.echo(chunk, writer, "_reasoning");
                            reasoningContent.append(chunk);
                        } else {
                            StreamEcho.text(chunk, writer);
                            deltaContent.append(chunk);
                        }
                    }
                }

                // [DONE]
                completions.addMessage(deltaContent.toString(), reasoningContent.toString(), Message.ROLE_AI);
                ChatStore.instance.store(completions);
                ChatStore.instance.storeAttach(chatRequest, null);
            }
        }
    }

    /**
     * @param chatRequest
     * @param prompt
     * @return
     */
    protected MessageCompletions getOrNewMessageCompletions(ChatRequest chatRequest, String prompt) {
        MessageCompletions c = null;
        if (chatRequest.getChatid() != null) {
            c = ChatStore.instance.get(chatRequest.getChatid());
            if (c == null) log.error("[getOrNewMessageCompletions] chat {} not found", chatRequest.getChatid());
        }

        if (c == null) {
            c = new MessageCompletions(prompt);
            // 第一句话作为主题
            c.setSubject(CommonsUtils.maxstr(chatRequest.getUserContent(false), 40));
            ChatStore.instance.store(c);
        }
        return c;
    }
}
