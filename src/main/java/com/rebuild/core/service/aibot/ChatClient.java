/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.persist4j.engine.ID;
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
     * @param chatRequest
     * @return
     */
    public Message post(ChatRequest chatRequest) {
        final String dsUrl = Config.getServerUrl("chat/completions");
        final String dsSecret = Config.getSecret();

        MessageCompletions completions = getOrNewMessageCompletions(chatRequest.getChatid(), Config.getBasePrompt());
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

        MessageCompletions completions = getOrNewMessageCompletions(chatRequest.getChatid(), Config.getBasePrompt());
        completions.addUserMessage(chatRequest);

        // "deepseek-reasoner"
        String reqBody = completions.toCompletions(true, null).toJSONString();
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
                    StreamEcho.text("请求接口错误:" + apiResp.code(), writer);
                    return;
                }

                // 推理 0=未开始 1=输出中 2=结束
                int reasoningState = 0;
                while (!source.exhausted()) {
                    String d = source.readUtf8Line();
                    if (Application.devMode()) System.out.println("[dev] " + d);
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
                                reasoning = "思考中...\n" + reasoning;
                                reasoningState = 1;
                            }
                            chunk = reasoning;
                        } else if (reasoningState == 1) {
                            reasoningState = 2;
                            StreamEcho.text("\n\n", writer);
                        }

                        if (chunk == null || chunk.isEmpty()) continue;

                        if (reasoningState == 1) {
                            StreamEcho.echo(chunk, writer, "reasoning");
                        } else {
                            StreamEcho.text(chunk, writer);
                        }
                        deltaContent.append(chunk);
                    }
                }

                // [DONE]
                completions.addMessage(deltaContent.toString(), Message.ROLE_AI);
                ChatStore.instance.store(completions);
            }
        }
    }

    /**
     * @param chatid
     * @param prompt
     * @return
     */
    public MessageCompletions getOrNewMessageCompletions(ID chatid, String prompt) {
        MessageCompletions c = null;
        if (chatid != null) {
            c = ChatStore.instance.get(chatid);
            if (c == null) log.error("[getOrNewMessageCompletions] chatid {} not found", chatid);
        }

        if (c == null) {
            c = new MessageCompletions(prompt);
            ChatStore.instance.store(c);
        }
        return c;
    }
}
