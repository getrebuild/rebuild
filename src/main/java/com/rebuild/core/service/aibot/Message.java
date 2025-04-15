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

    @Getter
    private MessageCompletions completions;

    @Getter
    private final String role;
    @Getter
    private final String content;
    @Getter
    private final String error;

    protected Message(String role, String content, String error, MessageCompletions completions) {
        this.role = role;
        this.content = content;
        this.error = error;
        this.completions = completions;
    }

    public boolean isSystem() {
        return "system".equals(role);
    }

    public JSONObject toJSON() {
        return JSONUtils.toJSONObject(
                new String[]{"role", "content"}, new Object[]{role, content});
    }

    public JSONObject toDeepChat(boolean hasRole) {
        if (error != null) {
            return JSONUtils.toJSONObject("error", error);
        }

        JSONObject o = JSONUtils.toJSONObject("text", content);
        if (hasRole) o.put("role", "user".equals(role) ? "user" : "ai");
        return o;
    }
}
