/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * 带格式的字段，例如电话、邮箱
 *
 * @author devezhao
 * @since 2025/9/15
 */
public interface PatternValue {

    /**
     * 字段正则表达式
     *
     * @return
     */
    Pattern getPattern();

    /**
     * 检查格式
     *
     * @param value
     * @return
     */
    default boolean checkPattern(String value) {
        if (StringUtils.isBlank(value)) return true;

        Pattern p = getPattern();
        if (p == null) return true;
        return p.matcher(value).matches();
    }
}
