/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.aibot;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class RequestBody {

    @Getter
    private final String chatid;
    @Getter
    private final JSONObject reqJson;

    protected RequestBody(HttpServletRequest request) {
        this.reqJson = (JSONObject) ServletUtils.getRequestJson(request);
        this.chatid = StringUtils.defaultIfBlank(request.getParameter("chatid"), null);
    }

    public String getUserContent() {
        return reqJson.getString("content");
    }

    public Object getUserAttach() {
        return reqJson.get("attach");
    }
}
