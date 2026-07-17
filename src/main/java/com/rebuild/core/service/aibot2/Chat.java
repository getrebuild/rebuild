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
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.services.blocking.chat.ChatCompletionService;
import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.service.aibot.StreamEcho;
import com.rebuild.core.service.aibot.tool.ToolDefs;
import com.rebuild.core.service.query.QueryHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.service.aibot2.Message.ROLE_AI;
import static com.rebuild.core.service.aibot2.Message.ROLE_USER;

/**
 * 会话
 *
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public class Chat implements Serializable {
    private static final long serialVersionUID = 471922851634230399L;

    private static final int MAX_TOOL_ROUNDS = 5;

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
        ChatCompletionCreateParams.Builder builder = requestParams(chatRequest.getUserContent(), chatRequest);
        ChatCompletion resp = completions().create(builder.build());
        ChatCompletionMessage ai = resp.choices().get(0).message();

        List<ChatCompletionMessageToolCall> toolCalls = ai.toolCalls().orElse(null);
        int maxRounds = MAX_TOOL_ROUNDS;
        while (CollectionUtils.isNotEmpty(toolCalls) && maxRounds-- > 0) {
            log.info("Tool calls round {} : {}", MAX_TOOL_ROUNDS - maxRounds, toolCalls.size());
            builder.addMessage(ai);

            for (ChatCompletionMessageToolCall tc : toolCalls) {
                ChatCompletionMessageFunctionToolCall fn = tc.asFunction();
                String toolCallId = fn.id();
                String fnName = fn.function().name();
                String fnArgs = fn.function().arguments();
                String toolResult = ToolDefs.execute(fnName, fnArgs);

                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCallId)
                        .content(toolResult)
                        .build());
            }

            resp = completions().create(builder.build());
            ai = resp.choices().get(0).message();
            toolCalls = ai.toolCalls().orElse(null);
        }

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

        ChatCompletionCreateParams.Builder builder = requestParams(chatRequest.getUserContent(), chatRequest);
        streamInternal(builder, writer, chatRequest, MAX_TOOL_ROUNDS);
    }

    /**
     * @param builder
     * @param writer
     * @param chatRequest
     * @param maxRounds
     */
    private void streamInternal(ChatCompletionCreateParams.Builder builder, PrintWriter writer,
                                ChatRequest chatRequest, int maxRounds) {
        StringBuilder fullContent = new StringBuilder();
        Map<Integer, String[]> toolCallAccumulator = new LinkedHashMap<>();

        try (StreamResponse<ChatCompletionChunk> resp = completions().createStreaming(builder.build())) {
            resp.stream().forEach(chunk -> chunk.choices().forEach(choice -> {
                String content = choice.delta().content().orElse("");
                if (StringUtils.isNotBlank(content)) {
                    StreamEcho.text(content, writer);
                    fullContent.append(content);
                }

                choice.delta().toolCalls().ifPresent(toolCalls -> {
                    for (com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall tc : toolCalls) {
                        int idx = (int) tc.index();
                        String[] entry = toolCallAccumulator.computeIfAbsent(idx, k -> new String[3]);
                        tc.id().ifPresent(id -> entry[0] = id);
                        tc.function().ifPresent(fn -> {
                            fn.name().ifPresent(name -> entry[1] = name);
                            fn.arguments().ifPresent(args -> {
                                entry[2] = entry[2] == null ? args : entry[2] + args;
                            });
                        });
                    }
                });

                // 中断
                if (StreamEcho.isInterrupted(chatRequest.getChatid())) {
                    log.warn("Chat interrupted : {}", chatRequest.getChatid());
                    resp.stream().close();
                }
            }));

            if (toolCallAccumulator.isEmpty() || maxRounds <= 0) {
                StreamEcho.echo(getChatid().toLiteral(), writer, "_chatid");
                completionAfter(fullContent.toString(), chatRequest);
                return;
            }

            log.info("Tool calls round {} : {}", MAX_TOOL_ROUNDS - maxRounds + 1, toolCallAccumulator.size());
            List<ChatCompletionMessageToolCall> assembledToolCalls = new ArrayList<>();
            for (String[] entry : toolCallAccumulator.values()) {
                String tcId = entry[0];
                String fnName = entry[1];
                String fnArgs = entry[2] == null ? "" : entry[2];

                ChatCompletionMessageFunctionToolCall fn = ChatCompletionMessageFunctionToolCall.builder()
                        .id(tcId)
                        .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                                .name(fnName)
                                .arguments(fnArgs)
                                .build())
                        .build();
                assembledToolCalls.add(ChatCompletionMessageToolCall.ofFunction(fn));
            }

            ChatCompletionMessage assistantMsg = ChatCompletionMessage.builder()
                    .content(fullContent.length() > 0 ? fullContent.toString() : null)
                    .refusal((String) null)
                    .toolCalls(assembledToolCalls)
                    .build();

            builder.addMessage(assistantMsg);

            for (String[] entry : toolCallAccumulator.values()) {
                String tcId = entry[0];
                String fnName = entry[1];
                String fnArgs = entry[2] == null ? "" : entry[2];
                String toolResult = ToolDefs.execute(fnName, fnArgs);

                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(tcId)
                        .content(toolResult)
                        .build());
            }

            streamInternal(builder, writer, chatRequest, maxRounds - 1);
        }
    }

    /**
     * 直接返回内容
     *
     * @param userMessage
     * @return
     */
    public String ask(String userMessage) {
        ChatCompletionCreateParams.Builder builder = requestParams(userMessage, null);
        ChatCompletion resp = completions().create(builder.build());
        ChatCompletionMessage ai = resp.choices().get(0).message();

        List<ChatCompletionMessageToolCall> toolCalls = ai.toolCalls().orElse(null);
        int maxRounds = MAX_TOOL_ROUNDS;
        while (CollectionUtils.isNotEmpty(toolCalls) && maxRounds-- > 0) {
            log.info("Tool calls round {} : {}", MAX_TOOL_ROUNDS - maxRounds, toolCalls.size());
            builder.addMessage(ai);

            for (ChatCompletionMessageToolCall tc : toolCalls) {
                ChatCompletionMessageFunctionToolCall fn = tc.asFunction();
                String toolCallId = fn.id();
                String fnName = fn.function().name();
                String fnArgs = fn.function().arguments();
                String toolResult = ToolDefs.execute(fnName, fnArgs);

                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCallId)
                        .content(toolResult)
                        .build());
            }

            resp = completions().create(builder.build());
            ai = resp.choices().get(0).message();
            toolCalls = ai.toolCalls().orElse(null);
        }

        return ai.content().orElse("");
    }

    private ChatCompletionService completions() {
        return Config.getClient().chat().completions();
    }

    /**
     * 构建参数
     *
     * @param userMessage
     * @param chatRequest
     * @return
     */
    private ChatCompletionCreateParams.Builder requestParams(String userMessage, ChatRequest chatRequest) {
        if (userMessage != null) {
            Message message = new Message(ROLE_USER, userMessage, null, null, chatRequest);
            messages.add(message);
        }

        // 合并 Skills 提示词
        String systemPrompt = prompt;
        if (chatRequest != null) {
            String skillPrompt = SkillDefs.getSystemPrompt(chatRequest.getSkill());
            if (skillPrompt != null) {
                systemPrompt = StringUtils.isBlank(systemPrompt) ? skillPrompt : systemPrompt + "\n\n" + skillPrompt;
            }
        }

        ChatCompletionCreateParams.Builder builder = Config.createBuilder(systemPrompt, model);
        for (Message m : messages) {
            String content = m.getContent();
            if (ROLE_USER.equals(m.getRole())) builder.addUserMessage(content);
            else if (ROLE_AI.equals(m.getRole())) builder.addAssistantMessage(content);
        }

        builder.tools(ToolDefs.tools())
                .toolChoice(ChatCompletionToolChoiceOption.Auto.AUTO);

        return builder;
    }

    /**
     * 完成后存储消息内容
     *
     * @param aiMessage
     * @param chatRequest
     * @return
     */
    private Message completionAfter(String aiMessage, ChatRequest chatRequest) {
        Message message = new Message(ROLE_AI, aiMessage, null, null, chatRequest);
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

            if (ROLE_USER.equals(role)) {
                // 附件
                ChatRequest chatRequest = new ChatRequest(msgJson, getChatid());
                content = chatRequest.getUserContent(true);

                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            } else if (ROLE_AI.equals(role)) {
                messages.add(new Message(role, content, null, null, getChatid(), msgJson));
            }
        }
    }
}
