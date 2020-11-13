/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.Serializable;

/**
 * Use fastjson
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/03
 */
public interface JSONable extends JSONAware, Serializable {

    /**
     * @return
     */
    JSON toJSON();

    /**
     * @param specFields
     * @return
     */
    default JSON toJSON(String... specFields) {
        return toJSON();
    }

    @Override
    default String toJSONString() {
        return JSON.toJSONString(toJSON());
    }
}
