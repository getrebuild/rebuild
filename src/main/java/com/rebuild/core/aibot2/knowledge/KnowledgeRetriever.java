/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.vector.VectorData;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识检索器：从知识库中检索与用户查询最相关的片段
 *
 * <p>轻量级实现：基于关键词匹配 + 简单 TF-IDF 打分，不依赖 Embedding API。
 * 未来可扩展为向量检索。</p>
 *
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
public class KnowledgeRetriever {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_KEYWORDS = 5;

    /**
     * 检索与查询最相关的知识片段
     */
    public static List<KnowledgeChunk> retrieve(String query, int topK) {
        if (StringUtils.isBlank(query) || query.trim().length() < MIN_QUERY_LENGTH) {
            return new ArrayList<>();
        }
        if (topK <= 0) topK = DEFAULT_TOP_K;

        List<String> keywords = TextChunkStrategy.extractKeywords(query);
        if (keywords.isEmpty()) {
            keywords.add(query.trim());
        }
        if (keywords.size() > MAX_KEYWORDS) {
            keywords = keywords.subList(0, MAX_KEYWORDS);
        }

        Map<ID, KnowledgeChunk> matched = queryChunks(keywords);

        attachKnowledgeName(matched);

        List<KnowledgeChunk> sorted = new ArrayList<>(matched.values());
        sorted.sort(Comparator.comparingDouble(KnowledgeChunk::getScore).reversed());

        List<KnowledgeChunk> result = sorted.size() > topK
                ? new ArrayList<>(sorted.subList(0, topK))
                : sorted;

        result = trimByContentLength(result);

        log.info("Knowledge retrieve: query='{}', keywords={}, matched={}, returned={}",
                StringUtils.abbreviate(query, 50), keywords.size(), matched.size(), result.size());

        return result;
    }

    /**
     * 检索并拼装为 VectorData（供 ChatRequest 自动注入）
     */
    public static VectorData retrieveAsVectorData(String query) {
        List<KnowledgeChunk> chunks = retrieve(query, DEFAULT_TOP_K);
        if (chunks.isEmpty()) return null;
        return new KnowledgeData(chunks);
    }

    /**
     * 单次查询所有关键词匹配的片段，使用 Map 去重并累加分数
     */
    private static Map<ID, KnowledgeChunk> queryChunks(List<String> keywords) {
        List<String> validKeywords = new ArrayList<>();
        for (String kw : keywords) {
            if (kw.length() >= MIN_QUERY_LENGTH) validKeywords.add(kw);
        }
        if (validKeywords.isEmpty()) return new LinkedHashMap<>();

        Object[][] results;

        if (CommandArgs.getBoolean(CommandArgs._UseDbFullText)) {
            String sql = "select CHUNK_ID, KNOWLEDGE_ID, CONTENT, CHUNK_INDEX, KEYWORDS " +
                    "from aibot_knowledge_chunk where KNOWLEDGE_ID in " +
                    "(select CONFIG_ID from aibot_commons_config where IS_DISABLED = 'F' and TYPE = 'KNOWLEDGE') " +
                    "and match(CONTENT) against (? in boolean mode)";
            String searchText = StringUtils.join(validKeywords, " ");
            results = Application.getQueryFactory()
                    .createNativeQuery(sql)
                    .setParameter(1, searchText)
                    .array();
        } else {
            StringBuilder where = new StringBuilder("(");
            boolean first = true;
            for (String kw : validKeywords) {
                String likeValue = "'%" + CommonsUtils.escapeSql(kw) + "%'";
                if (!first) where.append(" or ");
                first = false;
                where.append("(content like ").append(likeValue)
                        .append(" or keywords like ").append(likeValue).append(")");
            }
            where.append(")");

            String sql = "select chunkId, knowledgeId, content, chunkIndex, keywords " +
                    "from AibotKnowledgeChunk where knowledgeId in " +
                    "(select configId from AibotCommonsConfig where isDisabled = 'F' and type = 'KNOWLEDGE') and " + where;

            results = Application.createQueryNoFilter(sql).array();
        }

        Map<ID, KnowledgeChunk> matched = new LinkedHashMap<>();
        for (Object[] row : results) {
            ID chunkId = toId(row[0]);
            KnowledgeChunk chunk = matched.get(chunkId);
            if (chunk == null) {
                chunk = new KnowledgeChunk();
                chunk.setChunkId(chunkId);
                chunk.setKnowledgeId(toId(row[1]));
                chunk.setContent((String) row[2]);
                chunk.setChunkIndex(row[3] != null ? ((Number) row[3]).intValue() : 0);
                chunk.setKeywords((String) row[4]);
                chunk.setScore(0);
                matched.put(chunkId, chunk);
            }
            chunk.setScore(chunk.getScore() + scoreChunk(chunk, keywords));
        }

        return matched;
    }

    /**
     * 将查询结果中的 ID 值统一转为 ID 对象
     * （AJQL 查询返回 ID 对象，原生 SQL 查询返回 String）
     */
    private static ID toId(Object value) {
        if (value == null) return null;
        if (value instanceof ID) return (ID) value;
        return ID.valueOf(value.toString());
    }

    /**
     * 对片段打分：检查所有关键词的命中情况
     */
    private static double scoreChunk(KnowledgeChunk chunk, List<String> keywords) {
        double score = 0;
        String content = chunk.getContent() != null ? chunk.getContent().toLowerCase() : "";
        String kw = chunk.getKeywords() != null ? chunk.getKeywords().toLowerCase() : "";

        for (String keyword : keywords) {
            if (keyword.length() < MIN_QUERY_LENGTH) continue;
            String k = keyword.toLowerCase();

            if (kw.contains(k)) score += 2.0;
            if (content.contains(k)) score += 1.0;

            int occurrences = countOccurrences(content, k);
            if (occurrences > 1) score += Math.min(occurrences * 0.1, 1.0);
        }
        return score;
    }

    /**
     * 批量查询知识库名称（避免 N+1 查询）
     */
    private static void attachKnowledgeName(Map<ID, KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) return;

        List<ID> knowledgeIds = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks.values()) {
            if (chunk.getKnowledgeId() != null && !knowledgeIds.contains(chunk.getKnowledgeId())) {
                knowledgeIds.add(chunk.getKnowledgeId());
            }
        }
        if (knowledgeIds.isEmpty()) return;

        // in 列表直接拼接 ID 字面量（ID 来自自身查询结果，无注入风险）
        StringBuilder sql = new StringBuilder("select configId, name from AibotCommonsConfig where type = 'KNOWLEDGE' and configId in (");
        for (int i = 0; i < knowledgeIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("'").append(knowledgeIds.get(i)).append("'");
        }
        sql.append(")");

        Map<ID, String> nameMap = new HashMap<>();
        for (Object[] row : Application.createQueryNoFilter(sql.toString()).array()) {
            nameMap.put((ID) row[0], (String) row[1]);
        }

        for (KnowledgeChunk chunk : chunks.values()) {
            if (chunk.getKnowledgeId() != null) {
                chunk.setKnowledgeName(nameMap.get(chunk.getKnowledgeId()));
            }
        }
    }

    /**
     * 限制返回内容的总长度
     */
    private static List<KnowledgeChunk> trimByContentLength(List<KnowledgeChunk> chunks) {
        List<KnowledgeChunk> result = new ArrayList<>();
        int totalLen = 0;
        for (KnowledgeChunk chunk : chunks) {
            int contentLen = chunk.getContent() != null ? chunk.getContent().length() : 0;
            if (totalLen + contentLen > MAX_CONTENT_LENGTH) {
                if (result.isEmpty() && contentLen > 200) {
                    chunk.setContent(chunk.getContent().substring(0, Math.min(MAX_CONTENT_LENGTH, contentLen)) + "...");
                    result.add(chunk);
                }
                break;
            }
            totalLen += contentLen;
            result.add(chunk);
        }
        return result;
    }

    /**
     * 统计关键词在文本中出现次数
     */
    private static int countOccurrences(String text, String keyword) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(keyword)) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }
}
