/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.corpus.synonym.Synonym;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;
import com.hankcs.hanlp.dictionary.common.CommonSynonymDictionary;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从知识库中检索与用户查询最相关的片段
 * 基于关键词匹配 + 简单 TF-IDF 打分，不依赖 Embedding API；
 * 查询侧用 HanLP 内置同义词词典扩展关键词，无结果时用删字变体做错别字兜底
 *
 * @author devezhao
 * @since 2026/8/5
 */
@Slf4j
public class KnowledgeRetriever {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_KEYWORDS = 8;
    private static final int MAX_ORIGINAL_KEYWORDS = 5;
    private static final int MAX_SYNONYMS_PER_KEYWORD = 2;
    private static final int MAX_FUZZY_VARIANTS = 10;

    // 同义词命中、删字变体兜底命中的降权系数，保证精确命中排序优先
    private static final double SYNONYM_SCORE_FACTOR = 0.5;
    private static final double FUZZY_SCORE_FACTOR = 0.3;

    public static List<KnowledgeChunk> retrieve(String query, int topK) {
        return retrieve(query, topK, null);
    }

    /**
     * 检索与查询最相关的知识片段
     *
     * @param query
     * @param topK
     * @param scope
     * @return
     */
    public static List<KnowledgeChunk> retrieve(String query, int topK, Set<ID> scope) {
        if (StringUtils.isBlank(query) || query.trim().length() < MIN_QUERY_LENGTH) {
            return new ArrayList<>();
        }
        if (scope != null && scope.isEmpty()) return new ArrayList<>();
        if (topK <= 0) topK = DEFAULT_TOP_K;

        List<String> keywords = TextChunkStrategy.extractKeywords(query);
        if (keywords.isEmpty()) {
            keywords.add(query.trim());
        }
        if (keywords.size() > MAX_ORIGINAL_KEYWORDS) {
            keywords = keywords.subList(0, MAX_ORIGINAL_KEYWORDS);
        }

        Set<String> originalKeywords = new HashSet<>();
        for (String kw : keywords) originalKeywords.add(kw.toLowerCase());

        List<String> allKeywords = new ArrayList<>(keywords);
        for (String synonym : expandSynonyms(keywords)) {
            if (allKeywords.size() >= MAX_KEYWORDS) break;
            allKeywords.add(synonym);
        }

        Map<ID, KnowledgeChunk> matched = queryChunks(allKeywords, scope, originalKeywords);

        boolean fuzzyFallback = false;
        if (matched.isEmpty()) {
            matched = queryChunksByVariants(keywords, scope);
            fuzzyFallback = !matched.isEmpty();
        }

        attachKnowledgeName(matched);

        List<KnowledgeChunk> sorted = new ArrayList<>(matched.values());
        sorted.sort(Comparator.comparingDouble(KnowledgeChunk::getScore).reversed());

        List<KnowledgeChunk> result = sorted.size() > topK
                ? new ArrayList<>(sorted.subList(0, topK))
                : sorted;

        result = trimByContentLength(result);

        log.info("Knowledge retrieve: query='{}', keywords={}, synonyms={}, fuzzyFallback={}, matched={}, returned={}",
                StringUtils.abbreviate(query, 50), keywords.size(),
                allKeywords.size() - keywords.size(), fuzzyFallback, matched.size(), result.size());

        return result;
    }

    /**
     * 单次查询所有关键词匹配的片段，使用 Map 去重并累加分数
     *
     * @param keywords
     * @param scope
     * @param originalKeywords 原关键词（小写），非原关键词（同义词扩展）命中时降权
     * @return
     */
    private static Map<ID, KnowledgeChunk> queryChunks(List<String> keywords, Set<ID> scope, Set<String> originalKeywords) {
        List<String> validKeywords = new ArrayList<>();
        for (String kw : keywords) {
            if (kw.length() >= MIN_QUERY_LENGTH) validKeywords.add(CommonsUtils.escapeSql(kw));
        }
        if (validKeywords.isEmpty()) return new LinkedHashMap<>();

        String where;
        if (CommandArgs.getBoolean(CommandArgs._UseDbFullText)) {
            where = String.format("(content match '%s')", StringUtils.join(validKeywords, " "));
        } else {
            StringBuilder likes = new StringBuilder("(");
            for (String kw : validKeywords) {
                String likeValue = "'%" + kw + "%'";
                likes.append("(content like ").append(likeValue)
                        .append(" or keywords like ").append(likeValue).append(") or ");
            }
            where = likes.substring(0, likes.length() - 4) + ")";
        }

        String sql = "select chunkId,knowledgeId,content,chunkIndex,keywords" +
                " from AibotKnowledgeChunk where knowledgeId in" +
                " (select configId from AibotConfig where isDisabled = 'F' and type = 'KNOWLEDGE'" +
                buildScopeFilter(scope) + ") and " + where;
        Object[][] res = Application.createQueryNoFilter(sql).array();

        Map<ID, KnowledgeChunk> matched = new LinkedHashMap<>();
        for (Object[] row : res) {
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
            chunk.setScore(chunk.getScore() + scoreChunk(chunk, keywords, originalKeywords));
        }

        return matched;
    }

    /**
     * 用 HanLP 内置同义词词典（CoreSynonym）扩展关键词，每个原词最多取等义/近义词各若干；
     * 词典加载或查询失败时静默跳过，不影响主检索流程
     *
     * @param keywords
     * @return
     */
    private static List<String> expandSynonyms(List<String> keywords) {
        List<String> synonyms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String kw : keywords) seen.add(kw.toLowerCase());

        for (String kw : keywords) {
            try {
                CommonSynonymDictionary.SynonymItem item = CoreSynonymDictionary.get(kw);
                if (item == null || item.synonymList == null) continue;

                int added = 0;
                for (Synonym synonym : item.synonymList) {
                    if (added >= MAX_SYNONYMS_PER_KEYWORD) break;
                    if (synonym.type != Synonym.Type.EQUAL && synonym.type != Synonym.Type.LIKE) continue;
                    String word = synonym.realWord;
                    if (word == null || word.length() < MIN_QUERY_LENGTH) continue;
                    if (!seen.add(word.toLowerCase())) continue;
                    synonyms.add(word);
                    added++;
                }
            } catch (Exception ignored) {
            }
        }
        return synonyms;
    }

    /**
     * 错别字兜底：首轮无结果时，对含汉字的关键词逐位删一字生成变体，
     * 仅匹配 keywords 字段（分词/AI 增强关键词较集中，可控制单字误召回）并整体降权
     *
     * @param keywords
     * @param scope
     * @return
     */
    private static Map<ID, KnowledgeChunk> queryChunksByVariants(List<String> keywords, Set<ID> scope) {
        List<String> variants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String kw : keywords) {
            if (kw.length() < 2 || !TextChunkStrategy.containsCJK(kw)) continue;
            for (int i = 0; i < kw.length(); i++) {
                String variant = kw.substring(0, i) + kw.substring(i + 1);
                if (variant.isEmpty()) continue;
                if (seen.add(variant.toLowerCase())) variants.add(variant);
            }
        }
        if (variants.isEmpty()) return new LinkedHashMap<>();
        if (variants.size() > MAX_FUZZY_VARIANTS) {
            variants = variants.subList(0, MAX_FUZZY_VARIANTS);
        }

        StringBuilder likes = new StringBuilder("(");
        for (String variant : variants) {
            likes.append("(keywords like '%").append(CommonsUtils.escapeSql(variant)).append("%') or ");
        }
        String where = likes.substring(0, likes.length() - 4) + ")";

        String sql = "select chunkId,knowledgeId,content,chunkIndex,keywords" +
                " from AibotKnowledgeChunk where knowledgeId in" +
                " (select configId from AibotConfig where isDisabled = 'F' and type = 'KNOWLEDGE'" +
                buildScopeFilter(scope) + ") and " + where;
        Object[][] res = Application.createQueryNoFilter(sql).array();

        Map<ID, KnowledgeChunk> matched = new LinkedHashMap<>();
        for (Object[] row : res) {
            ID chunkId = toId(row[0]);
            if (matched.containsKey(chunkId)) continue;

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setChunkId(chunkId);
            chunk.setKnowledgeId(toId(row[1]));
            chunk.setContent((String) row[2]);
            chunk.setChunkIndex(row[3] != null ? ((Number) row[3]).intValue() : 0);
            chunk.setKeywords((String) row[4]);

            double score = 0;
            String kwField = chunk.getKeywords() != null ? chunk.getKeywords().toLowerCase() : "";
            for (String variant : variants) {
                if (kwField.contains(variant.toLowerCase())) score += 1.0;
            }
            chunk.setScore(score * FUZZY_SCORE_FACTOR);
            matched.put(chunkId, chunk);
        }

        log.info("Knowledge fuzzy fallback: variants={}, matched={}", variants.size(), matched.size());
        return matched;
    }

    private static String buildScopeFilter(Set<ID> scope) {
        if (scope == null) return "";
        List<String> ids = new ArrayList<>();
        for (ID id : scope) ids.add("'" + id.toLiteral() + "'");
        return " and configId in (" + StringUtils.join(ids, ",") + ")";
    }

    private static ID toId(Object value) {
        if (value == null) return null;
        if (value instanceof ID) return (ID) value;
        return ID.valueOf(value.toString());
    }

    private static double scoreChunk(KnowledgeChunk chunk, List<String> keywords, Set<String> originalKeywords) {
        double score = 0;
        String content = chunk.getContent() != null ? chunk.getContent().toLowerCase() : "";
        String kw = chunk.getKeywords() != null ? chunk.getKeywords().toLowerCase() : "";

        for (String keyword : keywords) {
            if (keyword.length() < MIN_QUERY_LENGTH) continue;
            String k = keyword.toLowerCase();

            double keywordScore = 0;
            if (kw.contains(k)) keywordScore += 2.0;
            if (content.contains(k)) keywordScore += 1.0;

            int occurrences = countOccurrences(content, k);
            if (occurrences > 1) keywordScore += Math.min(occurrences * 0.1, 1.0);

            if (!originalKeywords.contains(k)) keywordScore *= SYNONYM_SCORE_FACTOR;
            score += keywordScore;
        }
        return score;
    }

    // 批量查询知识库名称、源文档链接（避免 N+1 查询）
    private static void attachKnowledgeName(Map<ID, KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) return;

        List<ID> knowledgeIds = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks.values()) {
            if (chunk.getKnowledgeId() != null && !knowledgeIds.contains(chunk.getKnowledgeId())) {
                knowledgeIds.add(chunk.getKnowledgeId());
            }
        }
        if (knowledgeIds.isEmpty()) return;

        StringBuilder sql = new StringBuilder(
                "select configId,name,config from AibotConfig where type = 'KNOWLEDGE' and configId in (");
        for (int i = 0; i < knowledgeIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("'").append(knowledgeIds.get(i)).append("'");
        }
        sql.append(")");

        Map<ID, String> nameMap = new HashMap<>();
        Map<ID, String> linkMap = new HashMap<>();
        for (Object[] row : Application.createQueryNoFilter(sql.toString()).array()) {
            nameMap.put((ID) row[0], (String) row[1]);
            linkMap.put((ID) row[0], buildSourceLink((String) row[2]));
        }

        for (KnowledgeChunk chunk : chunks.values()) {
            if (chunk.getKnowledgeId() != null) {
                chunk.setKnowledgeName(nameMap.get(chunk.getKnowledgeId()));
                chunk.setSourceLink(linkMap.get(chunk.getKnowledgeId()));
            }
        }
    }

    /**
     * 构建文件型知识库的源文档预览链接（非文件型无文档可预览）
     *
     * @param configJson
     * @return
     */
    private static String buildSourceLink(String configJson) {
        JSONObject conf = (JSONObject) JSONUtils.parseSafe(configJson);
        if (conf == null || !KnowledgeBuilder.SOURCE_FILE.equals(conf.getString("sourceType"))) {
            return null;
        }

        JSONObject sourceConfig = (JSONObject) JSONUtils.parseSafe(conf.getString("sourceConfig"));
        String fileKey = sourceConfig != null ? sourceConfig.getString("file") : null;
        if (StringUtils.isBlank(fileKey)) return null;

        String fileUrl = String.format("/filex/download/%s?csrfToken=%s&attname=%s",
                fileKey, QiniuCloud.parseFileName(fileKey), AuthTokenManager.generateCsrfToken(90));
        fileUrl = AppUtils.getContextPath(fileUrl);
        return fileUrl;
    }

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
