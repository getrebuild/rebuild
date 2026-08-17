/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.knowledge.KnowledgeChunk;
import com.rebuild.core.aibot2.knowledge.KnowledgeRetriever;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 搜索系统知识库，获取与查询相关的知识片段
 *
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
public class SearchKnowledge implements Tool {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String query = args.getString("query");
        if (StringUtils.isBlank(query)) {
            throw new KnownToolException("搜索查询语句不能为空");
        }

        int topK = args.getIntValue("topK");
        if (topK < 1) topK = DEFAULT_TOP_K;
        if (topK > MAX_TOP_K) topK = MAX_TOP_K;

        List<KnowledgeChunk> chunks = KnowledgeRetriever.retrieve(query, topK);

        JSONArray results = new JSONArray();
        for (KnowledgeChunk chunk : chunks) {
            JSONObject item = new JSONObject();
            item.put("knowledgeName", chunk.getKnowledgeName());
            item.put("content", chunk.getContent());
            item.put("score", chunk.getScore());
            results.add(item);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "results", "total"},
                new Object[]{"ok", results, results.size()});
    }

    @Override
    public boolean isSystem() {
        return !Application.devMode();
    }
}
