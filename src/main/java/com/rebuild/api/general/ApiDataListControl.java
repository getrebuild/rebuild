/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.general.DataListBuilderImpl;
import com.rebuild.core.support.general.DataListWrapper;

/**
 * @author devezhao
 * @since 2020/5/21
 */
public class ApiDataListControl extends DataListBuilderImpl {

    /**
     * @param query
     * @param user
     */
    protected ApiDataListControl(JSONObject query, ID user) {
        super(query, user);
    }

    @Override
    protected DataListWrapper createDataListWrapper(int totalRows, Object[][] data, Query query) {
        return new ApiDataListWrapper(totalRows, data, query);
    }
}
