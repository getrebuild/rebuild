/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2025/4/29
 */
public class MarkdownTable {

    private List<String> headList = new ArrayList<>();
    private List<List<String>> rowDatas = new ArrayList<>();

    public MarkdownTable() {
    }

    public void addHead(String headName) {
        headList.add(headName);
    }

    public void addRowData(List<String> data) {
        rowDatas.add(data);
    }

    public String toMdTable() {
        StringBuilder md = new StringBuilder();

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
        for (List<String> row : rowDatas) {
            for (String cell : row) {
                md.append(String.format("| %s", escapeValue(cell)));
            }
            md.append(" |\n");
        }

        return md.toString();
    }

    // 替换 `|` 和换行符
    private String escapeValue(String md) {
        return md.replace("|", "\\|").replace("\n", " ");
    }
}
