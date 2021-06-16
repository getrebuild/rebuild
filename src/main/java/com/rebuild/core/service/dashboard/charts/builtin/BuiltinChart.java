/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

/**
 * 内置图表
 *
 * @author devezhao
 * @since 2019/10/14
 */
public interface BuiltinChart {

    /**
     * @return
     */
    default String getChartType() {
        return this.getClass().getSimpleName();
    }

    /**
     * @return
     */
    default JSONObject getChartConfig() {
        // 此处 entity=User 并无意义，只是便于权限控制
        return JSONUtils.toJSONObject(new String[]{"entity", "type"}, new String[]{"User", getChartType()});
    }

    /**
     * @return
     */
    ID getChartId();

    /**
     * @return
     */
    String getChartTitle();
}
