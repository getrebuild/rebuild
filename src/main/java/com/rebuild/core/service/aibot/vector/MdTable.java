/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.service.aibot.vector.VectorData.NN;

/**
 * @author Zixin
 * @since 2025/4/29
 */
public class MdTable {

    private String heading;
    private List<String> headList = new ArrayList<>();
    private List<String[]> rowDatas = new ArrayList<>();

    protected MdTable() {
        this(null);
    }

    protected MdTable(String heading) {
        this.heading = heading;
    }

    /**
     * @param headName
     * @return
     */
    public MdTable addHead(String headName) {
        headList.add(headName);
        return this;
    }

    /**
     * @param data
     * @return
     */
    public MdTable addRowData(List<String> data) {
        return addRowData(data.toArray(new String[0]));
    }

    /**
     * @param data
     * @return
     */
    public MdTable addRowData(String[] data) {
        rowDatas.add(data);
        return this;
    }

    /**
     * @return
     */
    public String toMdTable() {
        StringBuilder md = new StringBuilder();

        if (heading != null) {
            md.append("### ").append(heading).append(NN);
        }

        // HEAD
        for (String head : headList) {
            md.append(String.format("| %s", head));
        }
        md.append(" |\n");
        for (String ignored : headList) {
            md.append("|----");
        }
        md.append("|\n");

        // DATA
        for (String[] row : rowDatas) {
            for (String cell : row) {
                md.append(String.format("| %s", escapeValue(cell)));
            }
            md.append(" |\n");
        }

        return md.toString();
    }

    // 替换 `|` 和换行符
    private String escapeValue(String md) {
        if (StringUtils.isBlank(md)) return StringUtils.EMPTY;
        return md.replace("|", "\\|").replace("\n", " ");
    }
}
