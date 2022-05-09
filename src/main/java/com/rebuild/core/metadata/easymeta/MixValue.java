/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import com.alibaba.fastjson.JSONObject;

/**
 * 将复合值转为可识别值
 *
 * @author devezhao
 * @since 2020/11/17
 */
public interface MixValue {

    /**
     * 获取 Label/Text 人类可识别值
     *
     * @param wrappedValue
     * @return
     */
    default Object unpackWrapValue(Object wrappedValue) {
        if (wrappedValue instanceof JSONObject) {
            return ((JSONObject) wrappedValue).getString("text");
        }
        return null;
    }
}
