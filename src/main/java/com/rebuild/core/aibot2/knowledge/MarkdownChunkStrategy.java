/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 分片策略：按标题（##/###/####）分割，超长段落再用文本策略二次分片
 *
 * @author devezhao
 * @since 2026/8/5
 */
public class MarkdownChunkStrategy implements ChunkStrategy {

    private static final Pattern HEADING = Pattern.compile("(?m)^(#{2,4})\\s+(.+)$");

    @Override
    public List<Chunk> chunk(String content, int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();
        if (StringUtils.isBlank(content)) return chunks;
        if (maxChunkSize <= 0) maxChunkSize = DEFAULT_CHUNK_SIZE;

        content = content.trim();

        if (!HEADING.matcher(content).find()) {
            return new TextChunkStrategy().chunk(content, maxChunkSize);
        }

        // 零宽 lookahead 分割：保留标题行作为每个 section 的首行
        String[] sections = content.split("(?m)^(?=#{2,4}\\s)");

        int index = 0;
        for (String section : sections) {
            section = section.trim();
            if (StringUtils.isBlank(section)) continue;

            String title = extractTitle(section);

            if (section.length() > maxChunkSize) {
                // 超长 section 用文本策略二次分片
                List<Chunk> subChunks = new TextChunkStrategy().chunk(section, maxChunkSize);
                for (Chunk sc : subChunks) {
                    chunks.add(new Chunk(title != null ? title : sc.getTitle(),
                            sc.getContent(), index, sc.getKeywords()));
                    index++;
                }
            } else {
                chunks.add(new Chunk(title, section, index,
                        TextChunkStrategy.extractKeywords(section)));
                index++;
            }
        }

        return chunks;
    }

    /**
     * 从 section 首行提取标题文本
     */
    private String extractTitle(String section) {
        int nl = section.indexOf('\n');
        String firstLine = nl > 0 ? section.substring(0, nl).trim() : section;
        Matcher m = HEADING.matcher(firstLine);
        if (m.find()) return m.group(2).trim();
        return null;
    }
}
