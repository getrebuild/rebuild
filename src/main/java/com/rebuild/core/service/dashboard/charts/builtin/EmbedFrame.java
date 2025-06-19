/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.user.PageTokenVerify;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

/**
 * @author devezhao
 * @since 2025/2/20
 */
public class EmbedFrame extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000006");
    // 名称
    public static final String MYNAME = "EmbedFrame";

    public EmbedFrame() {
        super(null);
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("嵌入页面");
    }

    @Override
    public JSON build() {
        Object o = getExtraParams().get("extconfig");
        if (o != null) {
            String url = ((JSONObject) o).getString("url");
            if (url != null) url = PageTokenVerify.replacePageToken(url, getUser());
            return JSONUtils.toJSONObject("url", url);
        }

        return JSONUtils.clone(JSONUtils.EMPTY_OBJECT);
    }
}
