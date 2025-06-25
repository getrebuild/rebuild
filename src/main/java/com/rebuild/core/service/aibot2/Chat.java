/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.rebuild.core.Application;
import com.rebuild.core.service.aibot.AiBotException;
import com.rebuild.core.service.aibot.ChatRequest;
import com.rebuild.core.service.aibot.Config;
import com.rebuild.core.service.aibot.StreamEcho;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import static com.rebuild.core.service.aibot2.DeepSeek.getClient;

/**
 * @author Zixin
 * @since 2025/6/23
 */
@Slf4j
public class Chat {

    /**
     * @param request
     * @return
     */
    public static String post(ChatRequest request) {
        ChatCompletionCreateParams.Builder builder = getBuilder(request.getChatid());
        builder.addUserMessage(request.getUserContent());
        ChatCompletionCreateParams params = builder.build();

        ChatCompletion resp = getClient().chat().completions().create(params);
        ChatCompletionMessage ai = resp.choices().get(0).message();
        builder.addMessage(ai);
        return ai.content().orElse("");
    }

    /**
     * @param request
     * @param httpResp
     * @return
     */
    public static void stream(ChatRequest request, HttpServletResponse httpResp) {
        ChatCompletionCreateParams.Builder builder = getBuilder(request.getChatid());
        builder.addUserMessage(request.getUserContent());
        ChatCompletionCreateParams params = builder.build();

        PrintWriter writer;
        try {
            writer = httpResp.getWriter();
        } catch (IOException e) {
            throw new AiBotException("ERROR IN GETWRITER", e);
        }

        try (StreamResponse<ChatCompletionChunk> resp = getClient().chat().completions().createStreaming(params)) {
            resp.stream().forEach(chunk -> {
                chunk.choices().forEach(choice -> {
                    String ai = choice.delta().content().orElse("");
                    StreamEcho.text(ai, writer);
                });
            });
            StreamEcho.text("[DONE]", writer);
        }
    }

    /**
     * @param chatid
     * @return
     */
    public static ChatCompletionCreateParams.Builder getBuilder(ID chatid) {
        final String key = "chat-" + chatid;

        Serializable b = Application.getCommonsCache().getx(key);

        return DeepSeek.createBuilder(Config.getBasePrompt(), DeepSeek.MODEL_CHAT);
    }
}
