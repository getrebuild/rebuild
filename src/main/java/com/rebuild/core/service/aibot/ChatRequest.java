/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.aibot.vector.ListFilterData;
import com.rebuild.core.service.aibot.vector.RecordData;
import com.rebuild.core.service.aibot.vector.VectorData;
import com.rebuild.core.service.aibot.vector.VectorDataChunk;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class ChatRequest {

    @Getter
    private final ID chatid;
    @Getter
    private final String model;
    @Getter
    private final JSONObject reqJson;

    private String vectorDataContent;

    /**
     * @param request
     */
    public ChatRequest(HttpServletRequest request) {
        String id = request.getParameter("chatid");
        this.chatid = ID.isId(id) ? ID.valueOf(id) : null;
        this.model = request.getParameter("model");
        this.reqJson = (JSONObject) ServletUtils.getRequestJson(request);
    }

    /**
     * @return
     */
    public String getUserContent() {
        return getUserContent(true);
    }

    /**
     * @param withVector
     * @return
     */
    public String getUserContent(boolean withVector) {
        String c = reqJson.getString("content");
        if (!withVector) return c;

        String vdc = getVectorDataContent();
        if (vdc == null) return c;
        return vdc + "\n\n" + c;
    }

    /**
     * @return
     */
    protected String getVectorDataContent() {
        if (vectorDataContent == null) {
            VectorData vd = getVectorData();
            if (vd == null) return null;
            vectorDataContent = vd.toVector();
        }
        return vectorDataContent;
    }

    /**
     * @return
     */
    public VectorData getVectorData() {
        JSONArray attach = (JSONArray) reqJson.get("attach");
        if (CollectionUtils.isEmpty(attach)) return null;

        VectorDataChunk vdc = new VectorDataChunk();
        for (int i = 0; i < attach.size(); i++) {
            JSONObject item = attach.getJSONObject(i);

            String record = item.getString("record");
            String orListFilter = item.getString("listFilter");
            if (ID.isId(record)) {
                vdc.addVectorData(new RecordData(ID.valueOf(record)));
            } else if (JSONUtils.wellFormat(orListFilter)) {
                vdc.addVectorData(new ListFilterData(JSONObject.parseObject(orListFilter)));
            }
        }
        return vdc;
    }
}
