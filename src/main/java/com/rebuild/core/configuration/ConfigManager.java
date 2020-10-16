/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

/**
 * 配置管理器
 *
 * @author devezhao
 * @since 01/04/2019
 */
public interface ConfigManager {

    /**
     * 已删除项标记
     */
    String DELETED_ITEM = "$$RB_DELETED$$";

    /**
     * 清理缓存
     *
     * @param cacheKey
     */
    void clean(Object cacheKey);
}
