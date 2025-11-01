/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author devezhao
 * @since 2025/11/1
 */
@Getter
public class Message implements Serializable {
    private static final long serialVersionUID = 5985250499303228749L;

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "assistant";

    private final String role;
    private final String content;
    private final String reasoning;  // 思考
    private final String error;

    private ID userReqChatid;
    private JSON userReqJson;

    /**
     * @param role
     * @param content
     * @param reasoning
     * @param error
     * @param chatRequest
     */
    protected Message(String role, String content, String reasoning, String error, ChatRequest chatRequest) {
        this(role, content, reasoning, error, chatRequest.getChatid(), chatRequest.getReqJson());
    }

    /**
     * @param role
     * @param content
     * @param reasoning
     * @param error
     * @param userReqChatid
     * @param userReqJson
     */
    protected Message(String role, String content, String reasoning, String error, ID userReqChatid, JSON userReqJson) {
        this.role = role;
        this.content = content;
        this.reasoning = reasoning;
        this.error = error;
        this.userReqChatid = userReqChatid;
        this.userReqJson = userReqJson;
    }

    /**
     * @return
     */
    public JSONObject toSimpleJSON() {
        JSONObject o = JSONUtils.toJSONObject(
                new String[]{"role", "content"}, new Object[]{role, content});
        if (error != null) o.put("error", error);
        return o;
    }

    /**
     * @return
     */
    public JSONObject toJSON() {
        JSONObject d;
        if (Message.ROLE_USER.equals(role)) {
            d = (JSONObject) JSONUtils.clone(userReqJson);
        } else {
            d = toSimpleJSON();
            if (reasoning != null) d.put("reasoning", reasoning);
        }

        d.put("_chatid", userReqChatid);
        return d;
    }
}
