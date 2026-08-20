/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import java.util.List;

/**
 * 知识分片策略
 *
 * @author devezhao
 * @since 2026/8/5
 */
public interface ChunkStrategy {

    int DEFAULT_CHUNK_SIZE = 800;
    int DEFAULT_OVERLAP = 100;

    /**
     * 将内容分片
     *
     * @param content 原始内容
     * @param maxChunkSize 每片最大字符数
     * @return 分片列表
     */
    List<Chunk> chunk(String content, int maxChunkSize);

    /**
     * 分片结果
     */
    class Chunk {
        private final String title;
        private final String content;
        private final int index;
        private final List<String> keywords;

        public Chunk(String title, String content, int index, List<String> keywords) {
            this.title = title;
            this.content = content;
            this.index = index;
            this.keywords = keywords;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public int getIndex() { return index; }
        public List<String> getKeywords() { return keywords; }
    }
}
