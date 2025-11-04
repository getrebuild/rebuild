/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot2;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.aibot.vector.ListData;
import com.rebuild.core.service.aibot.vector.RecordData;
import com.rebuild.core.service.aibot.vector.VectorData;
import com.rebuild.core.service.aibot.vector.VectorDataChunk;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
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
        this.chatid = chatid;
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
                vdc.addVectorData(new ListData(JSONObject.parseObject(orListFilter)));
            }
        }
        return vdc;
    }

    /**
     * TODO 支持文件
     *
     * @return
     */
    public File[] getFile() {
        JSONArray filepath = (JSONArray) reqJson.get("file");
        if (CollectionUtils.isEmpty(filepath)) return null;

        List<File> files = new ArrayList<>();
        for (Object path : filepath) {
            File file = RebuildConfiguration.getFileOfTemp(path.toString());
            files.add(file);
        }
        return files.toArray(new File[0]);
    }
}
