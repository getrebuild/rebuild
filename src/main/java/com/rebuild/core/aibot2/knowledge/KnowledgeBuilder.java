/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.vector.FileData;
import com.rebuild.core.aibot2.vector.ListData;
import com.rebuild.core.aibot2.vector.RecordData;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.TransactionManual;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

/**
 * 知识库构建服务
 *
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
public class KnowledgeBuilder {

    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_RECORD = "RECORD";
    public static final String SOURCE_LIST = "LIST";
    public static final String SOURCE_URL = "URL";
    public static final String SOURCE_TEXT = "TEXT";

    private static final int MAX_CHUNK_SIZE = 1200;

    /**
     * 构建（或重建）知识库的分片
     *
     * @param knowledgeId
     * @param sourceType
     * @param sourceConfig
     * @param knowledgeName
     * @return
     */
    public static int build(ID knowledgeId, String sourceType, String sourceConfig, String knowledgeName) {
        String content = extractContent(sourceType, sourceConfig, knowledgeName);
        if (StringUtils.isBlank(content)) {
            throw new IllegalStateException("未提取到内容");
        }

        ChunkStrategy strategy = chooseStrategy(sourceType, content);
        List<ChunkStrategy.Chunk> chunks = strategy.chunk(content, MAX_CHUNK_SIZE);

        TransactionStatus tx = TransactionManual.newTransaction();
        try {
            Object[][] oldChunks = Application.createQueryNoFilter(
                    "select chunkId from AibotKnowledgeChunk where knowledgeId = ?")
                    .setParameter(1, knowledgeId)
                    .array();
            for (Object[] row : oldChunks) {
                Application.getCommonsService().delete((ID) row[0]);
            }

            for (ChunkStrategy.Chunk chunk : chunks) {
                RecordBuilder.builder(EntityHelper.AibotKnowledgeChunk)
                        .add("knowledgeId", knowledgeId)
                        .add("content", chunk.getContent())
                        .add("chunkIndex", chunk.getIndex())
                        .add("keywords", StringUtils.join(chunk.getKeywords(), ","))
                        .save(UserService.SYSTEM_USER);
            }

            updateChunkCount(knowledgeId, chunks.size());
            TransactionManual.commit(tx);
        } catch (Exception ex) {
            TransactionManual.rollback(tx);
            throw ex;
        }

        log.info("Knowledge built: name={}, chunks={}", knowledgeName, chunks.size());
        return chunks.size();
    }

    /**
     * 根据来源类型提取原始内容
     *
     * @param sourceType
     * @param sourceConfig
     * @param knowledgeName
     * @return
     */
    private static String extractContent(String sourceType, String sourceConfig, String knowledgeName) {
        if (StringUtils.isBlank(sourceConfig)) {
            log.warn("Empty sourceConfig for knowledge: {}", knowledgeName);
            return null;
        }

        JSONObject config = JSONObject.parseObject(sourceConfig);

        switch (sourceType) {
            case SOURCE_FILE: {
                String filePath = config.getString("file");
                if (StringUtils.isBlank(filePath)) return null;
                try {
                    return new FileData(filePath).toVector();
                } catch (Exception e) {
                    log.error("Failed to extract file content: {}", filePath, e);
                    return null;
                }
            }
            case SOURCE_RECORD: {
                String recordId = config.getString("record");
                if (!ID.isId(recordId)) return null;
                try {
                    return new RecordData(ID.valueOf(recordId)).toVector();
                } catch (Exception e) {
                    log.error("Failed to extract record content: {}", recordId, e);
                    return null;
                }
            }
            case SOURCE_LIST: {
                JSONObject listFilter = config.getJSONObject("listFilter");
                if (listFilter == null) return null;
                try {
                    return new ListData(listFilter).toVector();
                } catch (Exception e) {
                    log.error("Failed to extract list content", e);
                    return null;
                }
            }
            case SOURCE_URL: {
                String url = config.getString("url");
                if (StringUtils.isBlank(url)) return null;
                CommonsUtils.checkUrlSafe(url);
                try {
                    String html = OkHttpUtils.get(url);
                    return Jsoup.parse(html).text();
                } catch (Exception e) {
                    log.error("Failed to fetch URL: {}", url, e);
                    return null;
                }
            }
            case SOURCE_TEXT: {
                return config.getString("text");
            }
            default:
                log.warn("Unknown source type: {}", sourceType);
                return null;
        }
    }

    /**
     * 选择分片策略
     *
     * @param sourceType
     * @param content
     * @return
     */
    private static ChunkStrategy chooseStrategy(String sourceType, String content) {
        switch (sourceType) {
            case SOURCE_RECORD:
            case SOURCE_LIST:
                return new TableChunkStrategy();
            case SOURCE_TEXT:
            case SOURCE_URL:
                if (content.contains("\n## ") || content.contains("\n### ")) {
                    return new MarkdownChunkStrategy();
                }
                return new TextChunkStrategy();
            case SOURCE_FILE:
                if (content.contains("|---") || content.contains("| ---")) {
                    return new TableChunkStrategy();
                }
                if (content.contains("\n## ") || content.contains("\n### ")) {
                    return new MarkdownChunkStrategy();
                }
                return new TextChunkStrategy();
            default:
                return new TextChunkStrategy();
        }
    }

    /**
     * 更新知识库的分片数量
     *
     * @param knowledgeId
     * @param count
     */
    public static void updateChunkCount(ID knowledgeId, int count) {
        Object[] o = Application.createQueryNoFilter(
                "select config from AibotCommonsConfig where configId = ?")
                .setParameter(1, knowledgeId)
                .unique();

        JSONObject config = (o != null && o[0] != null)
                ? JSONUtils.parseObjectSafe((String) o[0]) : new JSONObject();
        if (config == null) config = new JSONObject();

        config.put("chunkCount", count);
        RecordBuilder.builder(knowledgeId)
                .add("config", config.toJSONString())
                .save(UserService.SYSTEM_USER);
    }
}
