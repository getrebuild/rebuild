/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

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
import com.rebuild.core.DefinedException;
import com.rebuild.core.aibot2.tool.KnownToolException;
import com.rebuild.core.aibot2.tool.ToolDefs;
import com.rebuild.core.service.approval.ApprovalException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.CommonsUtils;
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

import static com.rebuild.core.aibot2.Message.ROLE_AI;
import static com.rebuild.core.aibot2.Message.ROLE_USER;

/**
 * 会话
 *
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public class Chat implements Serializable {
    private static final long serialVersionUID = 471922851634230399L;

    private static final int MAX_TOOL_ROUNDS = 20;
    private static final String ROUNDS_LIMIT_NOTICE = "\n\n（本次对话的工具调用轮次已达上限，任务可能未完成。请发送\"继续\"以完成剩余步骤。）";

    @Getter
    private ID chatid;
    @Getter
    private String model;
    @Getter
    private String prompt;

    @Getter
    private List<Message> messages = new ArrayList<>();

    public Chat(ID chatid) {
        this(chatid, Config.getBasePrompt(), Config.getDefModel());
    }

    protected Chat(ID chatid, String prompt, String model) {
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

        ai = executeToolCalls(ai, builder);

        String content = ai.content().orElse("");
        // 轮次耗尽仍有未完成工具调用时给出提示
        if (ai.toolCalls().isPresent() && !ai.toolCalls().get().isEmpty()) {
            content += ROUNDS_LIMIT_NOTICE;
        }
        return completionAfter(content, chatRequest);
    }

    /**
     * @param chatRequest
     * @param httpResp
     */
    public void stream(ChatRequest chatRequest, HttpServletResponse httpResp) {
        // 清除残留中断标志，防止上一条消息的中断误伤本条
        StreamEcho.clearInterrupt(chatRequest.getChatid());

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

        // 先回传 chatid 供前端使用（如中断）
        StreamEcho.echo(getChatid().toLiteral(), writer, "_chatid");

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
        boolean[] interrupted = {false};
        boolean[] clientGone = {false};

        try (StreamResponse<ChatCompletionChunk> resp = completions().createStreaming(builder.build())) {
            try {
                resp.stream().forEach(chunk -> {
                    chunk.choices().forEach(choice -> {
                        String content = choice.delta().content().orElse("");
                        if (StringUtils.isNotBlank(content)) {
                            // 客户端断开后不再写入，但仍累积内容
                            if (!clientGone[0]) {
                                try {
                                    StreamEcho.text(content, writer);
                                } catch (Exception e) {
                                    clientGone[0] = true;
                                }
                                if (!clientGone[0] && writer.checkError()) {
                                    clientGone[0] = true;
                                }
                                if (clientGone[0]) {
                                    log.info("Client disconnected, continuing stream : {}", chatRequest.getChatid());
                                }
                            }
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
                    });

                    // 中断检查
                    if (StreamEcho.isInterrupted(chatRequest.getChatid())) {
                        log.warn("Chat interrupted : {}", chatRequest.getChatid());
                        interrupted[0] = true;
                        resp.stream().close();
                    }
                });

            } catch (Exception e) {
                if (!interrupted[0] && !clientGone[0]) throw e;
                log.debug("Stream closed (interrupt or client disconnect)");
            }

            if (interrupted[0] || toolCallAccumulator.isEmpty() || maxRounds <= 0) {
                String content = fullContent.toString();

                // 达到轮次上限且仍有待执行的工具调用，提示用户继续而非静默截断
                if (!interrupted[0] && maxRounds <= 0 && !toolCallAccumulator.isEmpty()) {
                    if (!clientGone[0]) StreamEcho.text(ROUNDS_LIMIT_NOTICE, writer);
                    content += ROUNDS_LIMIT_NOTICE;
                }

                completionAfter(content, chatRequest);
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
                String toolResult = safeExecute(fnName, fnArgs);

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

        ai = executeToolCalls(ai, builder);

        String content = ai.content().orElse("");
        // 轮次耗尽仍有未完成工具调用时给出提示
        if (ai.toolCalls().isPresent() && !ai.toolCalls().get().isEmpty()) {
            content += ROUNDS_LIMIT_NOTICE;
        }
        return content;
    }

    /**
     * 执行工具调用循环（post 和 ask 共用）
     *
     * @param ai
     * @param builder
     * @return 最终的 AI 消息
     */
    private ChatCompletionMessage executeToolCalls(ChatCompletionMessage ai, ChatCompletionCreateParams.Builder builder) {
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
                String toolResult = safeExecute(fnName, fnArgs);

                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCallId)
                        .content(toolResult)
                        .build());
            }

            ChatCompletion resp = completions().create(builder.build());
            ai = resp.choices().get(0).message();
            toolCalls = ai.toolCalls().orElse(null);
        }
        return ai;
    }

    /**
     * 安全执行工具调用，异常时返回错误信息而非中断会话
     *
     * @param toolName
     * @param arguments
     * @return
     */
    private String safeExecute(String toolName, String arguments) {
        try {
            return ToolDefs.execute(toolName, arguments);
        } catch (Exception ex) {
            // 异常日志已由 ToolDefs.execute 输出，此处不再重复记录

            // 系统已知业务异常（如数据校验失败）
            if (isKnownBusinessException(ex)) {
                String message = CommonsUtils.getRootMessage(ex);
                return "[业务校验错误] 此为系统已知的业务异常，请将以下错误信息如实反馈给用户，"
                        + "不要尝试修改数据或参数以绕过校验。\n错误信息: " + message;
            }

            // 工具层已知业务异常（如参数校验失败、实体不存在），可修正后重试
            if (ex instanceof KnownToolException) {
                return ex.getMessage();
            }

            return CommonsUtils.getRootMessage(ex);
        }
    }

    /**
     * 判断异常链中是否包含系统已知业务异常（DefinedException 及其子类，或 ApprovalException）
     *
     * @param ex
     * @return
     */
    private boolean isKnownBusinessException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof DefinedException || cause instanceof ApprovalException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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

        String systemPrompt = SystemPromptBuilder.build(prompt,
                chatRequest == null ? null : chatRequest.getSkill());

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
