/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import java.io.Serializable;

/**
 * @author devezhao
 * @since 10/12/2018
 */
public interface CacheTemplate<V extends Serializable> {

    /**
     * 1 分钟缓存
     */
    int TS_MINTE = 60;
    /**
     * 1小时缓存
     */
    int TS_HOUR = 60 * TS_MINTE;
    /**
     * 1天缓存
     */
    int TS_DAY = 24 * TS_HOUR;

    String get(String key);

    void put(String key, String value);

    void put(String key, String value, int seconds);

    V getx(String key);

    void putx(String key, V value);

    void putx(String key, V value, int seconds);

    void evict(String key);
}