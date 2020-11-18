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
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

/**
 * 多选数据
 *
 * @author devezhao
 * @since 2020/10/15
 * @see MultiSelectManager
 */
public class MultiSelectData extends BaseApi {

    @Override
    protected String getApiName() {
        return "metadata/multiselect-data";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String entity = context.getParameterNotBlank("entity");
        final String field = context.getParameterNotBlank("field");

        Field multiselectField = MetadataHelper.getField(entity, field);
        if (EasyMetaFactory.getDisplayType(multiselectField) != DisplayType.MULTISELECT) {
            throw new ApiInvokeException("Non multiselect field : " + entity + "." + field);
        }

        JSONArray data = MultiSelectManager.instance.getSelectList(multiselectField);
        return formatSuccess(data);
    }
}
