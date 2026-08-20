/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import cn.devezhao.persist4j.engine.ID;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识片段（检索结果）
 *
 * @author devezhao
 * @since 2026/8/5
 */
@Getter
@Setter
public class KnowledgeChunk {

    private ID chunkId;
    private ID knowledgeId;
    private String knowledgeName;
    private String content;
    private int chunkIndex;
    private String keywords;
    private double score;

    public KnowledgeChunk() {}

    public KnowledgeChunk(ID chunkId, String content, double score) {
        this.chunkId = chunkId;
        this.content = content;
        this.score = score;
    }
}
