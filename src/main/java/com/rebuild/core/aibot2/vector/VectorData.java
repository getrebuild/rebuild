/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.vector;

/**
 * 向模型推送的数据
 *
 * @author Zixin
 * @since 2025/4/18
 */
public interface VectorData {

    String N = "\n";
    String NN = "\n\n";

    int MAX_VECTOR_LENGTH = 65535;

    /**
     * @return
     */
    String toVector();

    /**
     * 将换行压平为空格，避免多行值破坏单行结构（键值对、表格单元格共用）
     *
     * @param text
     * @return
     */
    static String flatten(String text) {
        if (text == null) return "";
        return text.replace("\r", " ").replace("\n", " ");
    }

    /**
     * 超长内容截断，保留原始长度提示
     *
     * @param text
     * @return
     */
    static String truncate(String text) {
        if (text == null || text.length() <= MAX_VECTOR_LENGTH) return text;
        return text.substring(0, MAX_VECTOR_LENGTH)
                + NN + "... (内容已截断，原始长度 " + text.length() + " 字符)";
    }
}
