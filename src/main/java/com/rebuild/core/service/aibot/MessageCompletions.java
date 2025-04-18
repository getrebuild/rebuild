/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class MessageCompletions implements Serializable {
    private static final long serialVersionUID = -8886909310259876224L;

    @Getter
    @Setter
    private ID chatid;
    @Getter
    @Setter
    private String subject;

    @Getter
    private final List<Message> messages = new ArrayList<>();

    protected MessageCompletions(String prompt) {
        if (prompt != null) this.addMessage(prompt, "system");
    }

    /**
     * @param content
     * @param role
     * @return
     */
    public Message addMessage(String content, String role) {
        Message m = new Message(role, content, null, null, this);
        messages.add(m);
        return m;
    }

    /**
     * @param chatRequest
     * @return
     */
    public Message addUserMessage(ChatRequest chatRequest) {
        Message m = new Message(Message.ROLE_USER, chatRequest.getUserContent(true), null,
                chatRequest.getReqJson(), this);
        messages.add(m);
        return m;
    }

    /**
     * @param error
     * @return
     */
    public Message addError(String error) {
        Message m = new Message(null, null, error, null, this);
        messages.add(m);
        return m;
    }

    /**
     * @param rawMessage
     * @return
     */
    protected Message setRawMessage(JSONObject rawMessage) {
        Message m = new Message(
                rawMessage.getString("role"), rawMessage.getString("content"),
                rawMessage.getString("error"), rawMessage, this);
        messages.add(m);
        return m;
    }

    /**
     * @param stream
     * @return
     */
    public JSON toCompletions(boolean stream) {
        return toCompletions(stream, null);
    }

    /**
     * @param stream
     * @param model `deepseek-chat` `deepseek-reasoner`
     * @return
     */
    public JSON toCompletions(boolean stream, String model) {
        JSONObject data = Config.getDeepSeekParams();
        if (stream) data.put("stream", true);
        if (model != null) data.put("model", model);

        JSONArray ms = new JSONArray();
        for (Message message : messages) {
            if (message.getError() == null) ms.add(message.toJSON());
        }
        data.put("messages", ms);
        return data;
    }

    @Override
    public String toString() {
        return toCompletions(true, null).toString();
    }
}
