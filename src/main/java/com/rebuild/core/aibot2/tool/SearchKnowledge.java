/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.aibot2.knowledge.KnowledgeChunk;
import com.rebuild.core.aibot2.knowledge.KnowledgeRetriever;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.configuration.ConfigBean;
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

        // 未配置可用知识库时直接告知，与「有知识库但未匹配」区分开
        if (!hasEnabledKnowledge()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", "系统未配置知识库（或知识库均已禁用），请如实告知用户当前无知识库可搜索，不要编造内容"});
        }

        int topK = args.getIntValue("topK");
        if (topK < 1) topK = DEFAULT_TOP_K;
        if (topK > MAX_TOP_K) topK = MAX_TOP_K;

        List<KnowledgeChunk> chunks = KnowledgeRetriever.retrieve(query, topK);

        // 空结果附带引导，避免模型编造答案或盲目重试
        if (chunks.isEmpty()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message", "total"},
                    new Object[]{"ok", "未在知识库中检索到与查询相关的内容，请如实告知用户未找到，不要编造答案。可建议用户更换关键词后重试", 0});
        }

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

    /**
     * 是否存在已启用（未禁用）的知识库
     *
     * @return
     */
    private boolean hasEnabledKnowledge() {
        for (ConfigBean kb : AibotConfigManager.instance.getKnowledgeConfigs()) {
            if (!kb.getBoolean("isDisabled")) return true;
        }
        return false;
    }

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
