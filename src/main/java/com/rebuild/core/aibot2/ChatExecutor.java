/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.http.StreamResponse;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.services.blocking.chat.ChatCompletionService;
import com.rebuild.core.aibot2.ReasoningExtractor.FeedResult;
import com.rebuild.core.aibot2.ReasoningExtractor.ThinkTagParser;
import com.rebuild.core.aibot2.tool.ToolDefs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 非流式模型交互执行器（含工具调用循环与思考内容提取）
 *
 * @author Zixin
 * @since 2026/8/18
 */
@Slf4j
public class ChatExecutor {

    static final int MAX_TOOL_ROUNDS = 30;
    static final String ROUNDS_LIMIT_NOTICE = "\n\n（本次对话的工具调用轮次已达上限，任务可能未完成。请发送\"继续\"以完成剩余步骤。）";
    static final String TRUNCATED_NOTICE = "\n\n（本次回答因达到长度上限被截断，内容可能不完整。请发送\"继续\"以接着输出。）";

    private static final String[] NORMAL_FINISH_REASONS = {"stop", "tool_calls", "function_call"};

    private final Chat chat;
    private final ChatRequest chatRequest;
    private final ChatCompletionCreateParams.Builder builder;

    private String lastFinishReason;

    /**
     * @param chat
     * @param chatRequest
     * @param builder
     */
    public ChatExecutor(Chat chat, ChatRequest chatRequest, ChatCompletionCreateParams.Builder builder) {
        this.chat = chat;
        this.chatRequest = chatRequest;
        this.builder = builder;
    }

    /**
     * 执行并保存消息
     *
     * @return
     */
    public Message run() {
        ChatCompletion resp = createChat(builder.build(), chat.chatLogger());
        accumulateUsage(resp);
        ChatCompletion.Choice choice = resp.choices().get(0);
        ChatCompletionMessage ai = choice.message();
        lastFinishReason = finishReasonOf(choice);

        String[] reasoningAcc = {ReasoningExtractor.fromProps(ai._additionalProperties())};
        ai = executeToolCalls(ai, reasoningAcc);
        logFinishReasonIfAbnormal(lastFinishReason, chat.chatLogger());

        String reasoning = reasoningAcc[0];
        String content = ai.content().orElse("");
        // 无结构化思考字段时尝试 <think> 标签剥离
        if (StringUtils.isBlank(reasoning) && StringUtils.isNotBlank(content)) {
            FeedResult fr = splitThinkTags(content);
            reasoning = fr.getReasoning();
            content = StringUtils.defaultIfBlank(fr.getContent(), "");
        }

        content += roundsLimitNoticeIfNeed(ai) + truncatedNoticeIfNeed(ai);
        return chat.completionAfter(content, reasoning, chatRequest);
    }

    /**
     * 执行并直接返回内容（不保存消息）
     *
     * @return
     */
    public String runContent() {
        ChatCompletion resp = createChat(builder.build(), chat.chatLogger());
        accumulateUsage(resp);
        ChatCompletion.Choice choice = resp.choices().get(0);
        ChatCompletionMessage ai = choice.message();
        lastFinishReason = finishReasonOf(choice);

        ai = executeToolCalls(ai, new String[1]);
        logFinishReasonIfAbnormal(lastFinishReason, chat.chatLogger());
        return ai.content().orElse("") + roundsLimitNoticeIfNeed(ai) + truncatedNoticeIfNeed(ai);
    }

    /**
     * 轮次耗尽仍有未完成工具调用时给出提示
     *
     * @param ai
     * @return
     */
    private static String roundsLimitNoticeIfNeed(ChatCompletionMessage ai) {
        if (ai.toolCalls().isPresent() && !ai.toolCalls().get().isEmpty()) {
            return ROUNDS_LIMIT_NOTICE;
        }
        return "";
    }

    /**
     * 输出被长度上限截断且无工具调用时给出提示，避免半截回答被当作完整回答
     *
     * @param ai
     * @return
     */
    private String truncatedNoticeIfNeed(ChatCompletionMessage ai) {
        if (ai.toolCalls().isPresent() && !ai.toolCalls().get().isEmpty()) return "";
        return isLengthTruncated(lastFinishReason) ? TRUNCATED_NOTICE : "";
    }

    /**
     * @param finishReason
     * @return
     */
    static boolean isLengthTruncated(String finishReason) {
        return "length".equalsIgnoreCase(finishReason);
    }

    /**
     * 取结束原因，统一转为字符串以免流式/非流式两处枚举类型不一致
     *
     * @param choice
     * @return
     */
    private static String finishReasonOf(ChatCompletion.Choice choice) {
        try {
            return choice.finishReason().asString();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 结束原因异常时记录（同时写入系统日志与会话日志），正常结束不记以免噪音
     *
     * @param finishReason
     * @param chatLogger 可为 null
     */
    static void logFinishReasonIfAbnormal(String finishReason, ChatLogger chatLogger) {
        if (StringUtils.isBlank(finishReason)) return;
        for (String normal : NORMAL_FINISH_REASONS) {
            if (normal.equalsIgnoreCase(finishReason)) return;
        }

        String msg = "Abnormal finish reason : " + finishReason;
        log.warn(msg);
        if (chatLogger != null) chatLogger.logEvent(msg);
    }

    /**
     * 执行工具调用循环
     *
     * @param ai
     * @param reasoningAcc
     * @return
     */
    private ChatCompletionMessage executeToolCalls(ChatCompletionMessage ai, String[] reasoningAcc) {
        List<ChatCompletionMessageToolCall> toolCalls = ai.toolCalls().orElse(null);
        int maxRounds = MAX_TOOL_ROUNDS;
        while (CollectionUtils.isNotEmpty(toolCalls) && maxRounds-- > 0) {
            logToolCall(chat.chatLogger(), MAX_TOOL_ROUNDS - maxRounds + 1, toolCallsText(toolCalls));

            ai.content().ifPresent(c -> {
                if (StringUtils.isNotBlank(c)) chat.chatLogger().log("ASSISTANT", c);
            });
            builder.addMessage(ai);

            executeAndAppend(builder, toolCalls, chat.chatLogger());

            ChatCompletion resp = createChat(builder.build(), chat.chatLogger());
            accumulateUsage(resp);
            ChatCompletion.Choice choice = resp.choices().get(0);
            ai = choice.message();
            lastFinishReason = finishReasonOf(choice);

            String r = ReasoningExtractor.fromProps(ai._additionalProperties());
            if (StringUtils.isNotBlank(r)) {
                reasoningAcc[0] = StringUtils.isBlank(reasoningAcc[0]) ? r : reasoningAcc[0] + "\n" + r;
            }

            toolCalls = ai.toolCalls().orElse(null);
        }
        return ai;
    }

    /**
     * 执行工具调用并将结果加入请求上下文（流式/非流式共用）
     *
     * @param builder
     * @param toolCalls
     * @param chatLogger
     */
    static void executeAndAppend(ChatCompletionCreateParams.Builder builder,
                                 List<ChatCompletionMessageToolCall> toolCalls, ChatLogger chatLogger) {
        for (ChatCompletionMessageToolCall tc : toolCalls) {
            ChatCompletionMessageFunctionToolCall fn = tc.asFunction();
            String toolResult = ToolDefs.executeSafely(
                    fn.function().name(), fn.function().arguments(), chatLogger);

            builder.addMessage(ChatCompletionToolMessageParam.builder()
                    .toolCallId(fn.id())
                    .content(toolResult)
                    .build());
        }
    }

    /**
     * 记录工具调用轮次日志（流式/非流式共用）
     *
     * @param chatLogger
     * @param round
     * @param toolCallsText
     */
    static void logToolCall(ChatLogger chatLogger, int round, String toolCallsText) {
        chatLogger.logEvent(String.format("TOOL_CALL rounds %d/%d : %s", round, MAX_TOOL_ROUNDS, toolCallsText));
    }

    /**
     * 一次性 <think> 标签拆分（仅处理内容开头的标签）
     *
     * @param content
     * @return
     */
    static FeedResult splitThinkTags(String content) {
        ThinkTagParser parser = new ThinkTagParser();
        FeedResult fr = parser.feed(content);

        String dangling = parser.flushDangling();
        if (StringUtils.isNotBlank(dangling)) {
            String r = StringUtils.defaultString(fr.getReasoning()) + dangling;
            fr = new FeedResult(r, fr.getContent());
        }
        return fr;
    }

    /**
     * 采集响应中的 Token 用量（工具多轮每轮都计费，逐轮累加）
     *
     * @param resp
     */
    private void accumulateUsage(ChatCompletion resp) {
        resp.usage().ifPresent(u -> chat.addTokenUsage(u.totalTokens()));
    }

    /**
     * @return
     */
    static ChatCompletionService completions() {
        return Config.getClient().chat().completions();
    }

    /**
     * 调用 API，失败时转储请求内容便于定位上游拒绝原因
     *
     * @param params
     * @param chatLogger 可为 null
     * @return
     */
    static ChatCompletion createChat(ChatCompletionCreateParams params, ChatLogger chatLogger) {
        try {
            return completions().create(params);
        } catch (OpenAIServiceException ex) {
            logError(ex, params, chatLogger);
            throw ex;
        }
    }

    /**
     * 调用流式 API，失败时转储请求内容便于定位上游拒绝原因
     *
     * @param params
     * @param chatLogger 可为 null
     * @return
     */
    static StreamResponse<ChatCompletionChunk> createChatStreaming(ChatCompletionCreateParams params, ChatLogger chatLogger) {
        try {
            return completions().createStreaming(params);
        } catch (OpenAIServiceException ex) {
            logError(ex, params, chatLogger);
            throw ex;
        }
    }

    /**
     * 记录 API 错误及请求转储（同时写入系统日志与会话日志）
     *
     * @param ex
     * @param params
     * @param chatLogger 可为 null
     */
    private static void logError(OpenAIServiceException ex, ChatCompletionCreateParams params, ChatLogger chatLogger) {
        String requestJson = paramsToJson(params);
        log.error("Chat API error : {}\nREQUEST\n{}", ex.getMessage(), requestJson);
        if (chatLogger != null) {
            chatLogger.log("ERROR", "Chat API error : " + ex.getMessage() + "\n\n```json\n" + requestJson + "\n```");
        }
    }

    /**
     * @param params
     * @return
     */
    static String paramsToJson(ChatCompletionCreateParams params) {
        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (Exception ex) {
            return "(unserializable: " + ex.getMessage() + ")";
        }
    }

    /**
     * @param calls
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String toolCallsText(Object calls) {
        if (calls instanceof List) {
            List<String> texts = new ArrayList<>();
            for (ChatCompletionMessageToolCall tc : (List<ChatCompletionMessageToolCall>) calls) {
                ChatCompletionMessageFunctionToolCall.Function fn = tc.asFunction().function();
                texts.add(fn.name());
            }
            return String.join(", ", texts);
        }

        if (calls instanceof Map) {
            List<String> texts = new ArrayList<>();
            for (String[] entry : ((Map<Integer, String[]>) calls).values()) {
                texts.add(StringUtils.defaultString(entry[1]));
            }
            return String.join(", ", texts);
        }

        return calls.toString();
    }
}
