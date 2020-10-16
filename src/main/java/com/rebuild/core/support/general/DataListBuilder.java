/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public interface DataListBuilder {

    /**
     * @return
     */
    Entity getEntity();

    /**
     * 默认过滤条件
     *
     * @return
     */
    String getDefaultFilter();

    /**
     * JSON 结果集
     *
     * @return
     */
    JSON getJSONResult();
}
