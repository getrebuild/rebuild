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
 * 表格数据分片策略：按行数分割，每片最多 maxRows 行
 *
 * @author devezhao
 * @since 2026/8/5
 */
public class TableChunkStrategy implements ChunkStrategy {

    private static final int DEFAULT_MAX_ROWS = 20;

    private final int maxRows;

    public TableChunkStrategy() {
        this(DEFAULT_MAX_ROWS);
    }

    public TableChunkStrategy(int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<Chunk> chunk(String content, int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();
        if (StringUtils.isBlank(content)) return chunks;

        // 按行分割（表格数据每行一条记录）
        String[] lines = content.split("\n");
        if (lines.length == 0) return chunks;

        // 第一行通常是表头
        String header = lines[0];
        boolean hasTableHeader = header.contains("|") || header.contains("---");

        int index = 0;
        int dataStart = hasTableHeader ? 1 : 0;
        int pos = dataStart;

        while (pos < lines.length) {
            int end = Math.min(pos + maxRows, lines.length);
            StringBuilder sb = new StringBuilder();

            if (hasTableHeader && pos > dataStart) {
                sb.append(header).append("\n");
            }

            for (int i = pos; i < end; i++) {
                sb.append(lines[i]).append("\n");
            }

            String chunkContent = sb.toString().trim();
            if (StringUtils.isNotBlank(chunkContent)) {
                List<String> keywords = TextChunkStrategy.extractKeywords(chunkContent);
                String title = String.format("记录 %d-%d", pos - dataStart + 1, end - dataStart);
                chunks.add(new Chunk(title, chunkContent, index, keywords));
                index++;
            }

            pos = end;
        }

        return chunks;
    }
}
