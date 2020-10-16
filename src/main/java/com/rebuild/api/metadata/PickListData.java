/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;

/**
 * 下拉列表数据
 *
 * @author devezhao
 * @since 2020/10/15
 * @see PickListManager
 */
public class PickListData extends BaseApi {

    @Override
    protected String getApiName() {
        return "metadata/picklist-data";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String entity = context.getParameterNotBlank("entity");
        final String field = context.getParameterNotBlank("field");

        Field picklistField = MetadataHelper.getField(entity, field);
        if (EasyMeta.getDisplayType(picklistField) != DisplayType.PICKLIST) {
            throw new ApiInvokeException("Non picklist field : " + entity + "." + field);
        }

        JSONArray data = PickListManager.instance.getPickList(picklistField);
        return formatSuccess(data);
    }
}
