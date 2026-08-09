/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.knowledge;

import com.rebuild.core.aibot2.vector.VectorData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 将检索到的知识片段转为模型上下文（VectorData）
 *
 * @author devezhao
 * @since 2026/8/5
 */
public class KnowledgeData implements VectorData {

    private final List<KnowledgeChunk> chunks;

    public KnowledgeData(List<KnowledgeChunk> chunks) {
        this.chunks = chunks;
    }

    @Override
    public String toVector() {
        if (chunks == null || chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## 知识库参考信息").append(NN);

        for (KnowledgeChunk chunk : chunks) {
            if (StringUtils.isNotBlank(chunk.getKnowledgeName())) {
                sb.append("### 来源: ").append(chunk.getKnowledgeName()).append(NN);
            }

            sb.append(chunk.getContent()).append(NN);
        }

        sb.append("## 知识库参考信息结束").append(NN);
        return sb.toString();
    }
}
