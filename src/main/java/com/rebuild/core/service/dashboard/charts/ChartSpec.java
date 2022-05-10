/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.alibaba.fastjson.JSON;

/**
 * 图表规范
 *
 * @author devezhao
 * @since 2019/10/14
 */
public interface ChartSpec {

    /**
     * 构建图表数据
     *
     * @return
     */
    JSON build();
}
