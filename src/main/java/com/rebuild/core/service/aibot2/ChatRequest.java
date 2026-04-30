/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.aibot.vector.FileData;
import com.rebuild.core.service.aibot.vector.ListData;
import com.rebuild.core.service.aibot.vector.RecordData;
import com.rebuild.core.service.aibot.vector.VectorDataChunk;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 单次会话请求
 *
 * @author Zixin
 * @since 2025/11/1
 */
@Slf4j
public class ChatRequest {

    @Getter
    private final ID chatid;
    @Getter
    private final JSONObject reqJson;

    private String vectorDataContent;

    /**
     * @param request
     */
    public ChatRequest(HttpServletRequest request, ID chatid) {
        this((JSONObject) ServletUtils.getRequestJson(request), chatid);
    }

    /**
     * @param requestJson
     * @param chatid
     */
    public ChatRequest(JSONObject requestJson, ID chatid) {
        this.chatid = chatid;
        this.reqJson = requestJson;
    }

    /**
     * @return
     */
    public String getUserContent() {
        String c = getUserContent(true);
        if (Application.devMode()) System.out.println("[dev] \n" + c);
        return c;
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
        if (vectorDataContent != null) return vectorDataContent;

        JSONArray attach = (JSONArray) reqJson.get("attach");
        if (CollectionUtils.isEmpty(attach)) return null;

        String attachKey = attach.toJSONString();
        Object[] e = Application.createQueryNoFilter(
                "select vectorData from AibotChatAttach where chatId = ? and content = ?")
                .setParameter(1, chatid)
                .setParameter(2, attachKey)
                .unique();
        if (e != null) {
            vectorDataContent = (String) e[0];
            return vectorDataContent;
        }

        VectorDataChunk vdc = new VectorDataChunk();
        for (int i = 0; i < attach.size(); i++) {
            JSONObject item = attach.getJSONObject(i);

            String record = item.getString("record");
            String orListFilter = item.getString("listFilter");
            String orFile = item.getString("file");
            if (ID.isId(record)) {
                vdc.addVectorData(new RecordData(ID.valueOf(record)));
            } else if (JSONUtils.wellFormat(orListFilter)) {
                vdc.addVectorData(new ListData(JSONObject.parseObject(orListFilter)));
            } else if (StringUtils.isNotBlank(orFile)) {
                vdc.addVectorData(new FileData(orFile));
            }
        }

        vectorDataContent = StringUtils.trim(vdc.toVector());

        // 保存起来
        Record store = RecordBuilder.builder(EntityHelper.AibotChatAttach)
                .add("chatId", chatid)
                .add("content", attachKey)
                .add("vectorData", vectorDataContent)
                .build(UserService.SYSTEM_USER);
        store.setString("vectorData", vectorDataContent);
        Application.getCommonsService().create(store);

        return vectorDataContent;
    }
}
