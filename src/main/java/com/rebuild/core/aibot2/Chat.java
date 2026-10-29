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
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.rebuild.core.service.query.QueryHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.aibot2.ChatExecutor.toolCallsText;
import static com.rebuild.core.aibot2.Message.ROLE_AI;
import static com.rebuild.core.aibot2.Message.ROLE_USER;

/**
 * 会话（会话状态与消息持久化，模型交互由 {@link ChatExecutor} / {@link ChatStreamExecutor} 执行）
 *
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public class Chat implements Serializable {
    private static final long serialVersionUID = 471922851634230399L;

    @Getter
    private ID chatid;
    @Getter
    private volatile AibotAgent agent;
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
        this.agent = agent;
        this.restoreIfNeed();
        if (this.agent == null) this.agent = AibotAgent.defaultAgent();
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
     * @param agent
     */
    public synchronized void refreshAgent(AibotAgent agent) {
        if (agent != null) this.agent = agent;
    }

    /**
     * 尝试开始执行，已在执行中则等待释放（用于停止后立即发送的场景）
     *
     * @return
     */
    public synchronized boolean tryBeginRun() {
        long deadline = System.currentTimeMillis() + (10 * 1000);
        while (running) {
            long remains = deadline - System.currentTimeMillis();
            if (remains <= 0) return false;
            try {
                wait(remains);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        running = true;
        return true;
    }

    /**
     * 结束执行
     */
    public synchronized void endRun() {
        running = false;
        notifyAll();
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

        String systemPrompt = agent.buildSystemPrompt(null, false, false);
        chatLogger().logSession(agent.model(), systemPrompt);
        chatLogger().log("USER", userMessage);

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(systemPrompt, agent)
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
        boolean planMode = chatRequest != null && chatRequest.getPlanMode();
        boolean planConfirmed = chatRequest != null && chatRequest.getPlanConfirmed();
        String systemPrompt = agent.buildSystemPrompt(
                chatRequest == null ? null : chatRequest.getSkill(), planMode, planConfirmed);
        chatLogger().logSession(agent.model(), systemPrompt);

        if (userMessage != null) {
            Message message = new Message(ROLE_USER, userMessage, null, null, chatRequest);
            messages.add(message);

            chatLogger().log("USER", userMessage);
        }

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(systemPrompt, agent);
        for (Message m : requestMessages()) {
            String content = m.getContent();
            // 空内容消息（如仅附件或技能触发）发送给 AI 时需兑底文案，避免部分 AI 端拒绝空消息
            if (ROLE_USER.equals(m.getRole())) builder.addUserMessage(StringUtils.defaultIfBlank(content, "请开始"));
            else if (ROLE_AI.equals(m.getRole())) {
                String roundsText = appendToolHistory(builder, m.getToolHistory());
                content = stripRoundsPrefix(content, roundsText);
                builder.addAssistantMessage(content);
            }
        }

        builder.tools(agent.tools())
                .toolChoice(ChatCompletionToolChoiceOption.Auto.AUTO);

        return builder;
    }

    /**
     * 按记忆轮数截取发送给模型的历史消息（仅影响请求，不影响持久化与前端展示）
     * <p>按消息边界截取，工具调用与结果由 {@link #appendToolHistory} 在同一条 AI 消息内成对重建，不会产生孤儿 tool_call
     *
     * @return
     */
    private List<Message> requestMessages() {
        Long rounds = agent.memoryRounds();
        if (rounds == null) return messages;

        int allowed = rounds.intValue() + 1; // 历史轮数 + 本轮提问
        int users = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (ROLE_USER.equals(messages.get(i).getRole()) && ++users > allowed) {
                log.debug("Chat history truncated : chatid={}, keep={}/{}", getChatid(), messages.size() - i - 1, messages.size());
                return messages.subList(i + 1, messages.size());
            }
        }
        return messages;
    }

    /**
     * 重建历史消息中的工具调用与结果，使模型跨轮可见（避免重复查询与重复生成）
     *
     * @param builder
     * @param toolHistory
     * @return 各轮正文原文的拼接（供调用方剪除重复前缀），无有效轮返回 null
     */
    private String appendToolHistory(ChatCompletionCreateParams.Builder builder, JSONArray toolHistory) {
        if (toolHistory == null) return null;

        StringBuilder roundsText = new StringBuilder();
        for (Object o : toolHistory) {
            JSONObject round = (JSONObject) o;
            JSONArray calls = round.getJSONArray("calls");
            if (calls == null || calls.isEmpty()) {
                log.warn("Tool history round skipped (no calls) : chatid={}", getChatid());
                continue;
            }

            List<ChatCompletionMessageToolCall> toolCalls = new ArrayList<>();
            for (Object c : calls) {
                JSONObject call = (JSONObject) c;
                toolCalls.add(ChatCompletionMessageToolCall.ofFunction(
                        ChatCompletionMessageFunctionToolCall.builder()
                                .id(call.getString("id"))
                                .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                                        .name(call.getString("name"))
                                        .arguments(StringUtils.defaultString(call.getString("arguments")))
                                        .build())
                                .build()));
            }

            String rawContent = round.getString("content");
            if (rawContent != null) roundsText.append(rawContent);

            builder.addMessage(ChatCompletionMessage.builder()
                    .content(StringUtils.trimToNull(rawContent))
                    .refusal((String) null)
                    .toolCalls(toolCalls)
                    .build());

            JSONArray results = round.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                log.warn("Tool history round has no results : chatid={}, tools={}", getChatid(), toolCallsText(toolCalls));
                continue;
            }
            for (Object r : results) {
                JSONObject result = (JSONObject) r;
                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(result.getString("id"))
                        .content(StringUtils.defaultString(result.getString("result")))
                        .build());
            }
        }
        return roundsText.length() > 0 ? roundsText.toString() : null;
    }

    /**
     * 流式消息的正文为全部轮文本拼接，与工具轮记录的正文重复。重建时若正文以各轮拼接为前缀则剪去，仅保留末轮正文
     *
     * @param content
     * @param roundsText toolHistory 各轮正文拼接，可为 null
     * @return
     */
    private static String stripRoundsPrefix(String content, String roundsText) {
        if (StringUtils.isEmpty(roundsText) || content == null || !content.startsWith(roundsText)) return content;
        String remaining = content.substring(roundsText.length());
        // 剪除后为空时保持原文，避免发出空正文消息
        return StringUtils.isNotBlank(remaining) ? remaining : content;
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
        return completionAfter(aiMessage, reasoning, chatRequest, null);
    }

    /**
     * 完成后存储消息内容（含本轮工具调用与结果）
     *
     * @param aiMessage
     * @param reasoning
     * @param chatRequest
     * @param toolHistory 可为 null
     * @return
     */
    public Message completionAfter(String aiMessage, String reasoning, ChatRequest chatRequest, JSONArray toolHistory) {
        Message message = new Message(ROLE_AI, aiMessage, StringUtils.trimToNull(reasoning), null, chatRequest, toolHistory);
        messages.add(message);

        if (StringUtils.isNotBlank(reasoning)) chatLogger().log("REASONING", reasoning);
        chatLogger().log("ASSISTANT", aiMessage);

        this.store();
        return message;
    }

    /**
     * 完成后存储错误消息（带 error 标志，重新加载时仍渲染错误样式）
     *
     * @param errorMsg
     * @param chatRequest
     * @return
     */
    public Message completionError(String errorMsg, ChatRequest chatRequest) {
        Message message = new Message(ROLE_AI, errorMsg, null, errorMsg, chatRequest);
        messages.add(message);

        chatLogger().log("ASSISTANT", errorMsg);

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
        if (this.agent == null) {
            Object a = QueryHelper.queryFieldValue(getChatid(), "agentId");
            AibotAgent agent = a == null ? null : AgentDefs.getAgentQuietly((ID) a);
            if (agent != null) this.agent = agent;
        }

        Object t = QueryHelper.queryFieldValue(getChatid(), "token");
        if (t != null) this.tokenUsage = Long.parseLong(t.toString());

        Object o = QueryHelper.queryFieldValue(getChatid(), "contents");
        if (o == null) return;
        JSONArray data = JSONArray.parseArray((String) o);
        if (data == null) return;

        for (Object msg : data) {
            JSONObject msgJson = (JSONObject) msg;
            // 旧版 pub 会话的用户消息未存 role，此处兜底
            String role = StringUtils.defaultIfBlank(msgJson.getString("role"), ROLE_USER);
            String content = msgJson.getString("content");

            if (ROLE_USER.equals(role)) {
                ChatRequest chatRequest = new ChatRequest(msgJson, getChatid());
                content = chatRequest.getUserContent(true);

                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            } else if (ROLE_AI.equals(role)) {
                String reasoning = msgJson.getString("reasoning");
                String error = msgJson.getString("error");
                JSONArray toolHistory = msgJson.getJSONArray("toolHistory");
                messages.add(new Message(role, content, reasoning, error, getChatid(), msgJson, toolHistory));
            }
        }
    }
}
