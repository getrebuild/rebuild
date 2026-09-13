/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.vector;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.aibot2.Chat;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Message;
import org.apache.commons.lang3.StringUtils;

/**
 * 引用历史会话（AI 压缩后输出）
 *
 * @author dev
 * @since 2026/9/12
 */
public class RefChatData implements VectorData {

    private final ID chatid;

    public RefChatData(ID chatid) {
        this.chatid = chatid;
    }

    @Override
    public String toVector() {
        Chat chat = ChatManager.getChat(chatid);
        StringBuilder conversationText = new StringBuilder();
        for (Message m : chat.getMessages()) {
            String role = m.getRole();
            String content = m.getContent();
            if (StringUtils.isBlank(content)) continue;

            if (Message.ROLE_USER.equals(role)) {
                conversationText.append("用户: ").append(content).append(NN);
            } else if (Message.ROLE_AI.equals(role)) {
                conversationText.append("AI: ").append(content).append(NN);
            }
        }

        if (conversationText.length() == 0) return "";

        String prompt = "你是对话压缩助手，请将以下对话历史压缩为简洁摘要，保留关键信息和上下文要点";
        return ChatManager.ask(conversationText.toString(), prompt, null, "RefChatData");
    }
}
