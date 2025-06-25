package com.rebuild.core.service.aibot2;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.Test;

class ChatTest {

    @Test
    void createBuilder() {
        ChatCompletionCreateParams.Builder builder = DeepSeek.createBuilder(
                "你是一个MES专家", DeepSeek.MODEL_CHAT);
    }
}