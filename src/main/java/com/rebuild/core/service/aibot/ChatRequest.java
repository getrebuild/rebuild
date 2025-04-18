/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class ChatRequest {

    @Getter
    private final ID chatid;
    @Getter
    private final JSONObject reqJson;

    public ChatRequest(HttpServletRequest request) {
        String id = request.getParameter("chatid");
        this.chatid = ID.isId(id) ? ID.valueOf(id) : null;
        this.reqJson = (JSONObject) ServletUtils.getRequestJson(request);
    }

    public String getUserContent() {
        return reqJson.getString("content");
    }

    public Object getUserAttach() {
        return reqJson.get("attach");
    }

    public VectorData getVectorData() {
        return null;
    }
}
