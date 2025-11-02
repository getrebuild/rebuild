/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.services.blocking.chat.ChatCompletionService;
import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.service.aibot.Config;
import com.rebuild.core.service.aibot.StreamEcho;
import com.rebuild.core.service.query.QueryHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public class Chat implements Serializable {
    private static final long serialVersionUID = 471922851634230399L;

    @Getter
    private ID chatid;
    @Getter
    private String model;
    @Getter
    private String prompt;

    @Getter
    private List<Message> messages = new ArrayList<>();

    public Chat(ID chatid) {
        this(chatid, Config.getBasePrompt(), null);
    }

    public Chat(ID chatid, String prompt, String model) {
        this.chatid = chatid;
        this.model = model;
        this.prompt = prompt;
        this.restoreIfNeed();
    }

    /**
     * @param chatRequest
     * @return
     */
    public Message post(ChatRequest chatRequest) {
        ChatCompletionCreateParams params = buildRequestParams(chatRequest.getUserContent(), chatRequest);
        ChatCompletion resp = completions().create(params);
        ChatCompletionMessage ai = resp.choices().get(0).message();

        String content = ai.content().orElse("");
        return completionAfter(content, chatRequest);
    }

    /**
     * @param chatRequest
     * @param httpResp
     */
    public void stream(ChatRequest chatRequest, HttpServletResponse httpResp) {
        PrintWriter writer;
        try {
            writer = httpResp.getWriter();
        } catch (IOException e) {
            throw new AiBotException("ERROR IN GETWRITER", e);
        }

        httpResp.setContentType(org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE);
        httpResp.setCharacterEncoding("UTF-8");
        httpResp.setHeader("Cache-Control", "no-cache");
        httpResp.setHeader("Connection", "keep-alive");

        ChatCompletionCreateParams params = buildRequestParams(chatRequest.getUserContent(), chatRequest);
        StringBuilder fullContent = new StringBuilder();
        try (StreamResponse<ChatCompletionChunk> resp = completions().createStreaming(params)) {
            resp.stream().forEach(chunk -> chunk.choices().forEach(choice -> {
                String content = choice.delta().content().orElse("");
                StreamEcho.text(content, writer);
                fullContent.append(content);

                // TODO 不支持推理内容
            }));

            StreamEcho.echo(getChatid().toLiteral(), writer, "_chatid");
            completionAfter(fullContent.toString(), chatRequest);
        }
    }

    /**
     * 直接返回内容
     *
     * @param userMessage
     * @return
     */
    public String chat(String userMessage) {
        ChatCompletionCreateParams params = buildRequestParams(userMessage, null);
        ChatCompletion resp = completions().create(params);
        ChatCompletionMessage ai = resp.choices().get(0).message();
        return ai.content().orElse("");
    }

    private ChatCompletionService completions() {
        return DeepSeek.getClient().chat().completions();
    }

    private ChatCompletionCreateParams buildRequestParams(String userMessage, ChatRequest chatRequest) {
        Message message = new Message(Message.ROLE_USER, userMessage, null, null, chatRequest);
        messages.add(message);

        ChatCompletionCreateParams.Builder builder = DeepSeek.createBuilder(prompt, model);
        for (Message m : messages) {
            if (Message.ROLE_USER.equals(m.getRole())) builder.addUserMessage(m.getContent());
            else if (Message.ROLE_AI.equals(m.getRole())) builder.addAssistantMessage(m.getContent());
        }
        return builder.build();
    }

    private Message completionAfter(String aiMessage, ChatRequest chatRequest) {
        Message message = new Message(Message.ROLE_AI, aiMessage, null, null, chatRequest);
        messages.add(message);

        this.store();
        return message;
    }

    /**
     * 持久化
     */
    public void store() {
        ChatManager.storeChat(this);
    }

    /**
     * 恢复会话内容
     */
    protected void restoreIfNeed() {
        Object o = QueryHelper.queryFieldValue(getChatid(), "contents");
        if (o == null) return;
        JSONArray data = JSONArray.parseArray((String) o);
        if (data == null) return;

        for (Object msg : data) {
            JSONObject msgJson = (JSONObject) msg;
            String role = msgJson.getString("role");
            String content = msgJson.getString("content");

            if (Message.ROLE_USER.equals(role)) {
                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            } else if (Message.ROLE_AI.equals(role)) {
                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            }
        }
    }
}
