/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.CommonsUtils;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class MessageCompletions implements Serializable {

    @Getter
    private final String id;
    @Getter
    private final List<Message> messages = new ArrayList<>();

    protected MessageCompletions(String prompt) {
        id = "chat-" + CommonsUtils.randomHex(true);
        if (prompt != null) this.addMessage(prompt, "system");
    }

    /**
     * @param content
     * @param role system/user/assistant
     * @return
     */
    public Message addMessage(String content, String role) {
        Message m = new Message(role, content, null, this);
        messages.add(m);
        return m;
    }

    /**
     * @param error
     * @return
     */
    public Message addError(String error) {
        Message m = new Message(null, null, error, this);
        messages.add(m);
        return m;
    }

    /**
     * @param stream
     * @return
     */
    public JSON toCompletions(boolean stream) {
        JSONObject data = JSON.parseObject(DS_PARAM);
        data.put("model", RebuildConfiguration.get(ConfigurationItem.AibotDSSecret));
        if (stream) data.put("stream", true);

        JSONArray ms = new JSONArray();
        for (Message message : messages) {
            if (message.getError() == null) ms.add(message.toJSON());
        }
        data.put("messages", ms);
        return data;
    }

    @Override
    public String toString() {
        return toCompletions(true).toString();
    }

    /**
     */
    private static final String DS_PARAM = "{\n" +
            "    'model': 'deepseek-chat',\n" +
            "    'frequency_penalty': 0,\n" +
            "    'max_tokens': 2048,\n" +
            "    'presence_penalty': 0,\n" +
            "    'response_format': {\n" +
            "      'type': 'text'\n" +
            "    },\n" +
            "    'stop': null,\n" +
            "    'stream': false,\n" +
            "    'stream_options': null,\n" +
            "    'temperature': 1,\n" +
            "    'top_p': 1,\n" +
            "    'tools': null,\n" +
            "    'tool_choice': 'none',\n" +
            "    'logprobs': false,\n" +
            "    'top_logprobs': null\n" +
            "  }";
}
