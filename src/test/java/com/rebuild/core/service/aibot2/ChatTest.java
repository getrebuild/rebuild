package com.rebuild.core.service.aibot2;

import com.rebuild.core.ApplicationTest;
import org.junit.jupiter.api.Test;

class ChatTest extends ApplicationTest {

    @Test
    void chat() {
        System.out.println(ChatManager.ask("你是谁"));
    }
}