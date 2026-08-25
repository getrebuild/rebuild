/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import com.rebuild.core.service.query.QueryHelper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.aibot2.Message.ROLE_AI;
import static com.rebuild.core.aibot2.Message.ROLE_USER;

/**
 * 会话（会话状态与消息持久化，模型交互由 {@link ChatExecutor} / {@link ChatStreamExecutor} 执行）
 *
 * @author Zixin
 * @since 2025/11/1
 */
public class Chat implements Serializable {
    private static final long serialVersionUID = 471922851634230399L;

    @Getter
    private ID chatid;
    @Getter
    private AibotAgent agent;
    @Getter
    private List<Message> messages = new ArrayList<>();
    @Getter
    private long tokenUsage;

    private transient ChatLogger chatLogger;
    private transient volatile boolean running;

    public Chat(ID chatid) {
        this(chatid, null);
    }

    public Chat(ID chatid, AibotAgent agent) {
        this.chatid = chatid;
        this.agent = agent != null ? agent : AibotAgent.defaultAgent();
        this.restoreIfNeed();
    }

    protected Chat(ID chatid, String model, String prompt) {
        this(chatid, AibotAgent.defaultAgent(model, prompt));
    }

    /**
     * @param usage
     */
    public synchronized void addTokenUsage(long usage) {
        this.tokenUsage += usage;
    }

    /**
     * @return
     */
    public synchronized ChatLogger chatLogger() {
        if (chatLogger == null) chatLogger = new ChatLogger(chatid);
        return chatLogger;
    }

    /**
     * 尝试开始执行，已在执行中返回 false
     *
     * @return
     */
    public synchronized boolean tryBeginRun() {
        if (running) return false;
        running = true;
        return true;
    }

    /**
     * 结束执行
     */
    public synchronized void endRun() {
        running = false;
    }

    /**
     * @param chatRequest
     * @return
     */
    public Message post(ChatRequest chatRequest) {
        ChatCompletionCreateParams.Builder builder = requestParams(chatRequest.getUserContent(), chatRequest);
        return new ChatExecutor(this, chatRequest, builder).run();
    }

    /**
     * @param chatRequest
     * @param httpResp
     */
    public void stream(ChatRequest chatRequest, HttpServletResponse httpResp) {
        ChatCompletionCreateParams.Builder builder = requestParams(chatRequest.getUserContent(), chatRequest);
        new ChatStreamExecutor(this, chatRequest, builder).execute(httpResp);
    }

    /**
     * 直接返回内容
     *
     * @param userMessage
     * @return
     */
    public String ask(String userMessage) {
        ChatCompletionCreateParams.Builder builder = requestParams(userMessage, null);
        return new ChatExecutor(this, null, builder).runContent();
    }

    /**
     * 直接返回内容（多模态，如图片视觉识别）
     *
     * @param userMessage
     * @param parts
     * @return
     */
    public String ask(String userMessage, List<ChatCompletionContentPart> parts) {
        Message message = new Message(ROLE_USER, userMessage, null, null, null);
        messages.add(message);

        String systemPrompt = agent.buildSystemPrompt(null);
        chatLogger().logSession(agent.model(), systemPrompt);
        chatLogger().log("USER", userMessage);

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(systemPrompt, agent.model())
                .addUserMessageOfArrayOfContentParts(parts);
        return new ChatExecutor(this, null, builder).runContent();
    }

    /**
     * 构建参数
     *
     * @param userMessage
     * @param chatRequest
     * @return
     */
    private ChatCompletionCreateParams.Builder requestParams(String userMessage, ChatRequest chatRequest) {
        String systemPrompt = agent.buildSystemPrompt(
                chatRequest == null ? null : chatRequest.getSkill());
        chatLogger().logSession(agent.model(), systemPrompt);

        if (userMessage != null) {
            Message message = new Message(ROLE_USER, userMessage, null, null, chatRequest);
            messages.add(message);

            chatLogger().log("USER", userMessage);
        }

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(systemPrompt, agent.model());
        for (Message m : messages) {
            String content = m.getContent();
            if (ROLE_USER.equals(m.getRole())) builder.addUserMessage(content);
            else if (ROLE_AI.equals(m.getRole())) builder.addAssistantMessage(content);
        }

        builder.tools(agent.tools())
                .toolChoice(ChatCompletionToolChoiceOption.Auto.AUTO);

        return builder;
    }

    /**
     * 完成后存储消息内容
     *
     * @param aiMessage
     * @param reasoning
     * @param chatRequest
     * @return
     */
    public Message completionAfter(String aiMessage, String reasoning, ChatRequest chatRequest) {
        Message message = new Message(ROLE_AI, aiMessage, StringUtils.trimToNull(reasoning), null, chatRequest);
        messages.add(message);

        if (StringUtils.isNotBlank(reasoning)) chatLogger().log("REASONING", reasoning);
        chatLogger().log("ASSISTANT", aiMessage);

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
        Object t = QueryHelper.queryFieldValue(getChatid(), "token");
        if (t != null) this.tokenUsage = Long.parseLong(t.toString());

        Object o = QueryHelper.queryFieldValue(getChatid(), "contents");
        if (o == null) return;
        JSONArray data = JSONArray.parseArray((String) o);
        if (data == null) return;

        for (Object msg : data) {
            JSONObject msgJson = (JSONObject) msg;
            String role = msgJson.getString("role");
            String content = msgJson.getString("content");

            if (ROLE_USER.equals(role)) {
                ChatRequest chatRequest = new ChatRequest(msgJson, getChatid());
                content = chatRequest.getUserContent(true);

                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            } else if (ROLE_AI.equals(role)) {
                String reasoning = msgJson.getString("reasoning");
                messages.add(new Message(role, content, reasoning, null, getChatid(), msgJson));
            }
        }
    }
}
