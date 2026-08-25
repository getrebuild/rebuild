/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.rebuild.core.aibot2.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 使用 AI 为知识分片补充检索关键词（同义词、关联术语、用户可能的问法），
 * 弥补关键词匹配检索的语义缺口。构建期异步执行，单片失败不影响整体构建
 *
 * @author devezhao
 * @since 2026/8/25
 */
@Slf4j
public class KnowledgeKeywordEnricher {

    private static final int MAX_KEYWORDS_LENGTH = 1000;

    private static final String SYSTEM_PROMPT =
            "你是搜索关键词提取助手。针对给定的文档片段，生成补充检索关键词：包括同义词、近义词、" +
            "相关术语、常见缩写，以及用户检索该内容时可能使用的问法。" +
            "要求：只输出关键词，用英文逗号分隔，最多 20 个，不要输出任何解释、序号或换行。";

    /**
     * 为分片增强关键词（原地更新）
     *
     * @param chunks
     */
    public static void enrich(List<ChunkStrategy.Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;
        // 未配置 AI 时自动跳过，保留分词关键词
        if (!Config.availableAiBot()) return;

        int enriched = 0;
        for (ChunkStrategy.Chunk chunk : chunks) {
            try {
                List<String> aiKeywords = generateKeywords(chunk.getTitle(), chunk.getContent());
                if (!aiKeywords.isEmpty()) {
                    chunk.setKeywords(mergeKeywords(chunk.getKeywords(), aiKeywords));
                    enriched++;
                }
            } catch (Exception ex) {
                // 单片失败回退原有关键词，不中断构建
                log.warn("Knowledge keyword enrich failed for chunk#{} : {}", chunk.getIndex(), ex.getMessage());
            }
        }

        log.info("Knowledge keywords enriched by AI : {}/{}", enriched, chunks.size());
    }

    /**
     * @param title
     * @param content
     * @return
     */
    private static List<String> generateKeywords(String title, String content) {
        String input = StringUtils.isNotBlank(title) ? title + "\n\n" + content : content;

        ChatCompletionCreateParams params = Config.createBuilder(SYSTEM_PROMPT, null)
                .addUserMessage(input)
                .build();
        ChatCompletion resp = Config.getClient().chat().completions().create(params);
        String text = resp.choices().get(0).message().content().orElse("");

        List<String> keywords = new ArrayList<>();
        for (String kw : text.split("[,，;；\n]+")) {
            kw = kw.trim();
            if (kw.length() < 2 || kw.length() > 30) continue;
            keywords.add(kw);
        }
        return keywords;
    }

    /**
     * 合并分词关键词与 AI 关键词：去重（忽略大小写）并按入库长度截断
     *
     * @param baseKeywords
     * @param aiKeywords
     * @return
     */
    private static List<String> mergeKeywords(List<String> baseKeywords, List<String> aiKeywords) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> merged = new ArrayList<>();
        int length = 0;

        for (List<String> source : Arrays.asList(baseKeywords, aiKeywords)) {
            if (source == null) continue;
            for (String kw : source) {
                if (StringUtils.isBlank(kw)) continue;
                if (!seen.add(kw.toLowerCase())) continue;
                // +1 为逗号分隔符
                if (length + kw.length() + 1 > MAX_KEYWORDS_LENGTH) continue;
                merged.add(kw);
                length += kw.length() + 1;
            }
        }
        return merged;
    }
}
