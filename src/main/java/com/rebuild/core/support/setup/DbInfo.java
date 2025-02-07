/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

/**
 * @author RB
 * @since 2024/4/10
 */
public class DbInfo {

    final private String desc;

    protected DbInfo(String desc) {
        this.desc = desc;
    }

    public boolean isOceanBase() {
        return desc.contains("OceanBase");
    }

    public boolean isH2() {
        return desc.contains("H2");
    }

    public boolean isMySQL56() {
        if (isOceanBase()) return false;
        return desc.contains("5.6.");
    }

    public boolean isMySQL8x() {
        if (isOceanBase()) return false;
        return desc.startsWith("8.");
    }

    public boolean isMySQL9x() {
        if (isOceanBase()) return false;
        return desc.startsWith("9.");
    }

    public String getDesc() {
        return desc;
    }

    /**
     * @param L
     * @return
     */
    protected boolean isIgnoredSqlLine(String L) {
        if (isH2()) {
            // NOTE `double` 字段也不支持
            return L.startsWith("fulltext ") || L.startsWith("unique ") || L.startsWith("index ");
        }

        if (isOceanBase()) {
            return L.startsWith("fulltext ");
        }

        if (isMySQL56()) {
            // FIXME 针对实体，注意后续添加
            return L.startsWith("index IX0_attachment_folder");
        }

        return false;
    }
}
