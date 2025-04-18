/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 5985250499303228749L;

    @Getter
    private MessageCompletions messageCompletions;

    @Getter
    private final String role;
    @Getter
    private final String content;
    @Getter
    private final String error;

    protected Message(String role, String content, String error, MessageCompletions messageCompletions) {
        this.role = role;
        this.content = content;
        this.error = error;
        this.messageCompletions = messageCompletions;
    }

    public boolean isSystem() {
        return "system".equals(role);
    }

    public JSONObject toJSON() {
        JSONObject o = JSONUtils.toJSONObject(
                new String[]{"role", "content"}, new Object[]{role, content});
        if (error != null) o.put("error", error);
        return o;
    }

    public JSONObject toClientJSON() {
        JSONObject d = toJSON();
        d.put("_chatid", messageCompletions.getChatid());
        return d;
    }
}
