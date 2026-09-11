/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import com.rebuild.core.aibot2.ReasoningExtractor.FeedResult;
import com.rebuild.core.aibot2.ReasoningExtractor.ThinkTagParser;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rebuild.core.aibot2.ChatExecutor.MAX_TOOL_ROUNDS;
import static com.rebuild.core.aibot2.ChatExecutor.ROUNDS_LIMIT_NOTICE;
import static com.rebuild.core.aibot2.ChatExecutor.TRUNCATED_NOTICE;
import static com.rebuild.core.aibot2.ChatExecutor.createChatStreaming;
import static com.rebuild.core.aibot2.ChatExecutor.executeAndAppend;
import static com.rebuild.core.aibot2.ChatExecutor.isLengthTruncated;
import static com.rebuild.core.aibot2.ChatExecutor.logFinishReasonIfAbnormal;
import static com.rebuild.core.aibot2.ChatExecutor.logToolCall;
import static com.rebuild.core.aibot2.ChatExecutor.toolCallsText;

/**
 * 流式模型交互执行器（SSE 推送、工具调用循环与思考内容提取）
 *
 * @author Zixin
 * @since 2026/8/18
 */
@Slf4j
public class ChatStreamExecutor {

    // 思考内容推送上限。退化模型的思考可能极长且大量重复，超出后仅落库不再推送
    static final int MAX_REASONING_ECHO_CHARS = 9000;

    private final Chat chat;
    private final ChatRequest chatRequest;
    private final ChatCompletionCreateParams.Builder builder;

    // 跨轮次状态
    private final StringBuilder fullReasoning = new StringBuilder();
    // 跨轮次正文，与前端累加展示的内容一致，落库时取此值
    private final StringBuilder fullContentAll = new StringBuilder();
    private ThinkTagParser thinkParser = new ThinkTagParser();
    private boolean reasoningFound;
    private boolean reasoningEchoStopped;

    // 单轮次状态
    private final StringBuilder fullContent = new StringBuilder();
    private final Map<Integer, String[]> toolCallAccumulator = new LinkedHashMap<>();
    private boolean interrupted;
    private boolean clientGone;
    private String finishReason;

    private PrintWriter writer;

    /**
     * @param chat
     * @param chatRequest
     * @param builder
     */
    public ChatStreamExecutor(Chat chat, ChatRequest chatRequest, ChatCompletionCreateParams.Builder builder) {
        this.chat = chat;
        this.chatRequest = chatRequest;
        this.builder = builder;
    }

    /**
     * 执行流式输出
     *
     * @param httpResp
     */
    public void execute(HttpServletResponse httpResp) {
        // 清除残留中断标志，防止上一条消息的中断误伤本条
        StreamEcho.clearInterrupt(chatRequest.getChatid());

        try {
            this.writer = httpResp.getWriter();
        } catch (IOException e) {
            throw new AiBotException("ERROR IN GETWRITER", e);
        }

        httpResp.setContentType(org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE);
        httpResp.setCharacterEncoding("UTF-8");
        httpResp.setHeader("Cache-Control", "no-cache");
        httpResp.setHeader("Connection", "keep-alive");

        // 先回传 chatid 供前端使用（如中断）
        StreamEcho.echo(chat.getChatid().toLiteral(), writer, "_chatid");

        builder.streamOptions(ChatCompletionStreamOptions.builder()
                .includeUsage(true)
                .build());

        runRound(MAX_TOOL_ROUNDS);
    }

    /**
     * 执行单轮模型交互，有工具调用时递归下一轮
     *
     * @param maxRounds
     */
    private void runRound(int maxRounds) {
        fullContent.setLength(0);
        toolCallAccumulator.clear();
        interrupted = false;
        finishReason = null;
        reasoningFound = false;
        thinkParser = new ThinkTagParser();

        // API 调用前检查中断标志
        if (StreamEcho.isInterrupted(chatRequest.getChatid())) {
            interrupted = true;
            this.finish(maxRounds);
            return;
        }

        try (StreamResponse<ChatCompletionChunk> resp = createChatStreaming(builder.build(), chatLogger())) {
            try {
                resp.stream().forEach(chunk -> {
                    chunk.choices().forEach(choice -> {
                        // 思考内容：结构化字段优先，其次 <think> 标签剥离
                        String reasoningDelta = ReasoningExtractor.fromProps(choice.delta()._additionalProperties());
                        if (StringUtils.isNotBlank(reasoningDelta)) {
                            reasoningFound = true;
                            echoReasoning(reasoningDelta);
                        }

                        String content = choice.delta().content().orElse("");
                        if (StringUtils.isNotBlank(content) && !reasoningFound) {
                            FeedResult fr = thinkParser.feed(content);
                            if (StringUtils.isNotBlank(fr.getReasoning())) {
                                echoReasoning(fr.getReasoning());
                            }
                            content = fr.getContent();
                        }

                        if (StringUtils.isNotBlank(content)) {
                            echoText(content);
                            fullContent.append(content);
                            fullContentAll.append(content);
                        }

                        choice.delta().toolCalls().ifPresent(toolCalls -> {
                            for (ChatCompletionChunk.Choice.Delta.ToolCall tc : toolCalls) {
                                accumulateToolCall(tc);
                            }
                        });

                        choice.finishReason().ifPresent(fr -> finishReason = fr.asString());
                    });

                    chunk.usage().ifPresent(u -> chat.addTokenUsage(u.totalTokens()));

                    if (StreamEcho.isInterrupted(chatRequest.getChatid())) {
                        chatLogger().logEvent("Chat interrupted");
                        interrupted = true;
                        resp.stream().close();
                    }
                });

            } catch (Exception e) {
                if (!interrupted && !clientGone) throw e;
                chatLogger().logEvent("Stream closed (interrupt or client disconnect)");
            }

            logFinishReasonIfAbnormal(finishReason, chatLogger());

            if (interrupted || toolCallAccumulator.isEmpty() || maxRounds <= 0) {
                this.finish(maxRounds);
                return;
            }

            logToolCall(chatLogger(), MAX_TOOL_ROUNDS - maxRounds + 1, toolCallsText(toolCallAccumulator));
            if (fullContent.length() > 0) {
                chatLogger().log("ASSISTANT", fullContent.toString());
            }

            if (!this.appendToolMessages()) {
                this.finish(maxRounds);
                return;
            }
            this.runRound(maxRounds - 1);
        }
    }

    /**
     * 结束流式输出并保存消息
     *
     * @param maxRounds
     */
    private void finish(int maxRounds) {
        String dangling = thinkParser.flushDangling();
        if (StringUtils.isNotBlank(dangling)) echoReasoning(dangling);

        String content = fullContentAll.toString();

        if (!interrupted && maxRounds <= 0 && !toolCallAccumulator.isEmpty()) {
            echoText(ROUNDS_LIMIT_NOTICE);
            content += ROUNDS_LIMIT_NOTICE;
        } else if (!interrupted && toolCallAccumulator.isEmpty() && isLengthTruncated(finishReason)) {
            echoText(TRUNCATED_NOTICE);
            content += TRUNCATED_NOTICE;
        }

        chat.completionAfter(content,
                fullReasoning.length() > 0 ? fullReasoning.toString() : null, chatRequest);
    }

    /**
     * 组装本轮工具调用并加入请求上下文
     *
     * @return
     */
    private boolean appendToolMessages() {
        List<ChatCompletionMessageToolCall> assembledToolCalls = new ArrayList<>();
        for (String[] entry : toolCallAccumulator.values()) {
            if (StringUtils.isBlank(entry[1])) {
                String msg = "Malformed tool call dropped : " + Arrays.toString(entry);
                log.warn(msg);
                chatLogger().logEvent(msg);
                continue;
            }

            String toolCallId = StringUtils.isBlank(entry[0])
                    ? "call_" + CommonsUtils.randomHex(true)
                    : entry[0];

            ChatCompletionMessageFunctionToolCall fn = ChatCompletionMessageFunctionToolCall.builder()
                    .id(toolCallId)
                    .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                            .name(entry[1])
                            .arguments(entry[2] == null ? "" : entry[2])
                            .build())
                    .build();
            assembledToolCalls.add(ChatCompletionMessageToolCall.ofFunction(fn));
        }

        if (assembledToolCalls.isEmpty()) return false;

        ChatCompletionMessage assistantMsg = ChatCompletionMessage.builder()
                .content(fullContent.length() > 0 ? fullContent.toString() : null)
                .refusal((String) null)
                .toolCalls(assembledToolCalls)
                .build();
        builder.addMessage(assistantMsg);

        executeAndAppend(builder, assembledToolCalls, chatLogger(), this::echoTool);
        return true;
    }

    /**
     * 累积流式工具调用片段
     *
     * @param tc
     */
    private void accumulateToolCall(ChatCompletionChunk.Choice.Delta.ToolCall tc) {
        int idx = (int) tc.index();
        String[] entry = toolCallAccumulator.computeIfAbsent(idx, k -> new String[3]);
        tc.id().ifPresent(id -> entry[0] = id);
        tc.function().ifPresent(fn -> {
            fn.name().ifPresent(name -> {
                if (StringUtils.isNotBlank(name)) entry[1] = name;
            });
            fn.arguments().ifPresent(args -> entry[2] = entry[2] == null ? args : entry[2] + args);
        });
    }

    /**
     * 推送思考内容（客户端断开或超出推送上限后仅累积）
     *
     * @param reasoningDelta
     */
    private void echoReasoning(String reasoningDelta) {
        fullReasoning.append(reasoningDelta);

        if (reasoningEchoStopped || clientGone) return;
        if (fullReasoning.length() > MAX_REASONING_ECHO_CHARS) {
            reasoningEchoStopped = true;
            chatLogger().logEvent("Reasoning echo stopped (over " + MAX_REASONING_ECHO_CHARS + " chars)");
            return;
        }

        try {
            StreamEcho.echo(reasoningDelta, writer, "_reasoning");
        } catch (Exception e) {
            clientGone = true;
        }
        if (!clientGone && writer.checkError()) {
            clientGone = true;
        }
        if (clientGone) {
            chatLogger().logEvent("Client disconnected, continuing stream");
        }
    }

    /**
     * 推送正文内容（客户端断开后仅累积）
     *
     * @param content
     */
    private void echoText(String content) {
        if (clientGone) return;

        try {
            StreamEcho.text(content, writer);
        } catch (Exception e) {
            clientGone = true;
        }
        if (!clientGone && writer.checkError()) {
            clientGone = true;
        }
        if (clientGone) {
            chatLogger().logEvent("Client disconnected, continuing stream");
        }
    }

    /**
     * 推送工具执行进度（客户端断开后忽略）
     *
     * @param hint
     */
    private void echoTool(String hint) {
        if (clientGone) return;

        try {
            StreamEcho.echo(hint, writer, "_tool");
        } catch (Exception e) {
            clientGone = true;
        }
        if (!clientGone && writer.checkError()) {
            clientGone = true;
        }
        if (clientGone) {
            chatLogger().logEvent("Client disconnected, continuing stream");
        }
    }

    /**
     * @return
     */
    private ChatLogger chatLogger() {
        return chat.chatLogger();
    }
}
