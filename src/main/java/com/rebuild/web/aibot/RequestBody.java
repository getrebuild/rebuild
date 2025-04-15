/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class RequestBody {

    @Getter
    private final String chatid;
    private final JSONObject reqJson;

    protected RequestBody(HttpServletRequest request) {
        this.reqJson = (JSONObject) ServletUtils.getRequestJson(request);
        this.chatid = request.getHeader("chatid");
    }

    public String getUserContent() {
        JSONArray messages = reqJson.getJSONArray("messages");
        return messages.getJSONObject(0).getString("text");
    }
}
