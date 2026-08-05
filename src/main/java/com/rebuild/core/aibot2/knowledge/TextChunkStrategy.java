/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
     * 提取关键词：按标点空白分词，CJK 文本使用 bigram 滑窗（无需分词库）
     */
    public static List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (StringUtils.isBlank(text)) return keywords;

        String[] words = text.split("[\\s\\p{Punct}\uFF0C\u3002\uFF01\uFF1F\uFF1B\uFF1A\u3001\u201C\u201D\u2018\u2019\uFF08\uFF09\u3010\u3011\u300A\u300B]+");
        for (String word : words) {
            String w = word.trim();
            if (w.length() < 2) continue;

            if (containsCJK(w)) {
                for (int i = 0; i + 2 <= w.length() && i < 30; i++) {
                    String bigram = w.substring(i, i + 2);
                    if (!keywords.contains(bigram)) keywords.add(bigram);
                }
            } else if (w.length() <= 20 && !keywords.contains(w)) {
                keywords.add(w);
            }
        }

        if (keywords.size() > 30) {
            keywords = new ArrayList<>(keywords.subList(0, 30));
        }
        return keywords;
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
