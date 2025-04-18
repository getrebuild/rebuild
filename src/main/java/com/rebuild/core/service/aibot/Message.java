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

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "assistant";

    @Getter
    private MessageCompletions messageCompletions;

    @Getter
    private final String role;
    @Getter
    private final String content;
    @Getter
    private final String error;

    private final JSONObject originRequest;

    /**
     * @param role
     * @param content
     * @param error
     * @param originRequest
     * @param messageCompletions
     */
    protected Message(String role, String content, String error,
                      JSONObject originRequest, MessageCompletions messageCompletions) {
        this.role = role;
        this.content = content;
        this.error = error;
        this.messageCompletions = messageCompletions;
        this.originRequest = originRequest;
    }

    /**
     * @return
     */
    public boolean isSystem() {
        return ROLE_SYSTEM.equals(role);
    }

    /**
     * @return
     */
    public JSONObject toJSON() {
        JSONObject o = JSONUtils.toJSONObject(
                new String[]{"role", "content"}, new Object[]{role, content});
        if (error != null) o.put("error", error);
        return o;
    }

    /**
     * @return
     */
    public JSONObject toClientJSON() {
        JSONObject d;
        if (Message.ROLE_USER.equals(role)) {
            d = (JSONObject) JSONUtils.clone(originRequest);
        } else {
            d = toJSON();
        }
        d.put("_chatid", messageCompletions.getChatid());
        return d;
    }
}
