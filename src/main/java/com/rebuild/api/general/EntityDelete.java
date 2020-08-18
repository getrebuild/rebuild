/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;

/**
 * 删除记录
 *
 * @author devezhao
 * @since 2020/5/21
 */
public class EntityDelete extends BaseApi {

    @Override
    protected String getApiName() {
        return "entity/delete";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final ID deleteId = context.getParameterAsId("id");
        final Entity entity = MetadataHelper.getEntity(deleteId.getEntityCode());
        if (!entity.isQueryable() || MetadataHelper.isBizzEntity(entity.getEntityCode())) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Unsupportted operation for entity/id : " + deleteId);
        }

        int deleted = Application.getService(entity.getEntityCode()).delete(deleteId);

        return formatSuccess(JSONUtils.toJSONObject("deleted", deleted));
    }
}
