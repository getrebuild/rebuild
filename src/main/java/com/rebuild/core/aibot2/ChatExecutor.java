/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.services.blocking.chat.ChatCompletionService;
import com.rebuild.core.aibot2.ReasoningExtractor.FeedResult;
import com.rebuild.core.aibot2.ReasoningExtractor.ThinkTagParser;
import com.rebuild.core.aibot2.tool.ToolDefs;
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
public class ChatExecutor {

    static final int MAX_TOOL_ROUNDS = 20;
    static final String ROUNDS_LIMIT_NOTICE = "\n\n（本次对话的工具调用轮次已达上限，任务可能未完成。请发送\"继续\"以完成剩余步骤。）";

    private final Chat chat;
    private final ChatRequest chatRequest;
    private final ChatCompletionCreateParams.Builder builder;

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
        ChatCompletion resp = completions().create(builder.build());
        accumulateUsage(resp);
        ChatCompletionMessage ai = resp.choices().get(0).message();

        String[] reasoningAcc = {ReasoningExtractor.fromProps(ai._additionalProperties())};
        ai = executeToolCalls(ai, reasoningAcc);

        String reasoning = reasoningAcc[0];
        String content = ai.content().orElse("");
        // 无结构化思考字段时尝试 <think> 标签剥离
        if (StringUtils.isBlank(reasoning) && StringUtils.isNotBlank(content)) {
            FeedResult fr = splitThinkTags(content);
            reasoning = fr.getReasoning();
            content = StringUtils.defaultIfBlank(fr.getContent(), "");
        }

        content += roundsLimitNoticeIfNeed(ai);
        return chat.completionAfter(content, reasoning, chatRequest);
    }

    /**
     * 执行并直接返回内容（不保存消息）
     *
     * @return
     */
    public String runContent() {
        ChatCompletion resp = completions().create(builder.build());
        accumulateUsage(resp);
        ChatCompletionMessage ai = resp.choices().get(0).message();

        ai = executeToolCalls(ai, new String[1]);
        return ai.content().orElse("") + roundsLimitNoticeIfNeed(ai);
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
     * 执行工具调用循环
     *
     * @param ai
     * @param reasoningAcc 思考内容累积器（单元素数组，跨轮次累积）
     * @return 最终的 AI 消息
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

            ChatCompletion resp = completions().create(builder.build());
            accumulateUsage(resp);
            ai = resp.choices().get(0).message();

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
     * @param calls
     * @return
     */
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
