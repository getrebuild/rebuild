/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文本分片策略：按段落分割，每片不超过 maxChunkSize，相邻片之间有 overlap 重叠
 *
 * @author devezhao
 * @since 2026/8/5
 */
public class TextChunkStrategy implements ChunkStrategy {

    private static final String[] PARAGRAPH_DELIMITERS = {"\n\n", "\n", "。", "！", "？", ". ", "! ", "? "};

    @Override
    public List<Chunk> chunk(String content, int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();
        if (StringUtils.isBlank(content)) return chunks;
        if (maxChunkSize <= 0) maxChunkSize = DEFAULT_CHUNK_SIZE;

        content = content.trim();

        if (content.length() <= maxChunkSize) {
            chunks.add(new Chunk(null, content, 0, extractKeywords(content)));
            return chunks;
        }

        int overlap = Math.min(DEFAULT_OVERLAP, maxChunkSize / 4);
        int index = 0;
        int pos = 0;

        while (pos < content.length()) {
            int end = Math.min(pos + maxChunkSize, content.length());

            // 尝试在边界处找到段落分隔符，避免截断句子
            if (end < content.length()) {
                int bestBreak = findBestBreak(content, pos, end, maxChunkSize);
                if (bestBreak > pos) end = bestBreak;
            }

            String chunkContent = content.substring(pos, end).trim();
            if (StringUtils.isNotBlank(chunkContent)) {
                chunks.add(new Chunk(null, chunkContent, index, extractKeywords(chunkContent)));
                index++;
            }

            if (end >= content.length()) break;

            // 前移 pos，保留 overlap
            pos = end - overlap;
            if (pos < 0) pos = 0;
        }

        return chunks;
    }

    /**
     * 在 [start, end] 范围内寻找最佳断点（最后一个段落分隔符）
     */
    private int findBestBreak(String content, int start, int end, int maxChunkSize) {
        for (String delimiter : PARAGRAPH_DELIMITERS) {
            int idx = content.lastIndexOf(delimiter, end);
            if (idx > start + maxChunkSize / 2) {
                return idx + delimiter.length();
            }
        }
        return -1;
    }

    /**
     * 提取关键词：HanLP 分词后按权重提取，附加英文/数字词；
     * HanLP 提取为空时回退 bigram 滑窗保证短文本可用（构建与查询共用，需保持一致）
     */
    public static List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (StringUtils.isBlank(text)) return keywords;
        Set<String> seen = new HashSet<>();

        // HanLP 提取的关键词已按权重排序（含停用词过滤）
        try {
            for (String kw : HanLP.extractKeyword(text, 15)) {
                addKeyword(keywords, seen, kw);
            }
        } catch (Exception ignored) {
        }

        // 补充英文/数字词（产品名、字段名等，HanLP 可能忽略）
        try {
            for (Term term : HanLP.segment(text)) {
                addKeyword(keywords, seen, term.word);
            }
        } catch (Exception ignored) {
        }

        if (keywords.size() > 30) {
            keywords = new ArrayList<>(keywords.subList(0, 30));
        }

        // 短文本 HanLP 可能提取不出关键词，回退 bigram 滑窗兜底（无需分词库）
        if (keywords.isEmpty()) {
            for (String w : text.split("[\\s\\p{Punct}\uFF0C\u3002\uFF01\uFF1F\uFF1B\uFF1A\u3001\u201C\u201D\u2018\u2019\uFF08\uFF09\u3010\u3011\u300A\u300B]+")) {
                if (w.trim().length() < 2) continue;
                if (!containsCJK(w)) continue;
                for (int i = 0; i + 2 <= w.length() && i < 10; i++) {
                    addKeyword(keywords, seen, w.substring(i, i + 2));
                }
            }
        }

        return keywords;
    }

    /**
     * 添加关键词：仅保留纯字母/数字词或含汉字的词，去重（忽略大小写）
     */
    private static void addKeyword(List<String> keywords, Set<String> seen, String word) {
        if (StringUtils.isBlank(word)) return;
        String w = word.trim();
        if (w.length() < 2 || w.length() > 20) return;
        if (!containsCJK(w)) {
            boolean allLetterOrDigit = true;
            for (int i = 0; i < w.length(); i++) {
                if (!Character.isLetterOrDigit(w.charAt(i))) {
                    allLetterOrDigit = false;
                    break;
                }
            }
            if (!allLetterOrDigit) return;
        }
        if (seen.add(w.toLowerCase())) keywords.add(w);
    }

    /**
     * 判断字符串是否包含 CJK 汉字
     */
    private static boolean containsCJK(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) return true;
        }
        return false;
    }
}
