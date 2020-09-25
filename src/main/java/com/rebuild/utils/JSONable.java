/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/03
 */
public interface JSONable {

    /**
     * @return
     */
    JSON toJSON();

    /**
     * @param special
     * @return
     */
    default JSON toJSON(String... special) {
        return toJSON();
    }
}
