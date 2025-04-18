/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import com.rebuild.core.service.aibot.VectorData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2025/4/18
 */
public class VectorDataChunk implements VectorData {

    private List<VectorData> datas = new ArrayList<>();

    public void addVectorData(VectorData data) {
        datas.add(data);
    }

    @Override
    public String toVector() {
        StringBuilder out = new StringBuilder();
        for (VectorData data : datas) {
            out.append(data.toVector()).append("\n\n");
        }
        return out.append('\n').toString();
    }
}
