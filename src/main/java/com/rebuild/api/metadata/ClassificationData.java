/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.utils.JSONUtils;

/**
 * 分类数据
 *
 * @author devezhao
 * @since 2020/5/14
 * @see ClassificationManager
 */
public class ClassificationData extends BaseApi {

    private ID dataId;
    private int openLevel;

    @Override
    protected String getApiName() {
        return "metadata/classification-data";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String entity = context.getParameterNotBlank("entity");
        final String field = context.getParameterNotBlank("field");

        Field classField = MetadataHelper.getField(entity, field);
        if (EasyMetaFactory.getDisplayType(classField) != DisplayType.CLASSIFICATION) {
            throw new ApiInvokeException("Non classification field : " + entity + "." + field);
        }

        dataId = ClassificationManager.instance.getUseClassification(classField, true);
        if (dataId == null) {
            throw new ApiInvokeException("Bad classification field : " + entity + "." + field);
        }
        openLevel = ClassificationManager.instance.getOpenLevel(classField);

        Object[][] array = Application.createQueryNoFilter(
                "select itemId,name from ClassificationData where level = 0 and dataId = ?")
                .setParameter(1, dataId)
                .array();

        JSONArray dest = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = buildItem(o);
            appendChildren((ID) o[0], item, 1);
            dest.add(item);
        }

        return formatSuccess(dest);
    }

    private void appendChildren(ID itemId, JSONObject into, int level) {
        if (level > openLevel) {
            return;
        }

        Object[][] array = Application.createQueryNoFilter(
                "select itemId,name from ClassificationData where dataId = ? and parent = ?")
                .setParameter(1, dataId)
                .setParameter(2, itemId)
                .array();

        JSONArray children = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = buildItem(o);
            appendChildren((ID) o[0], item, level + 1);
            children.add(item);
        }
        into.put("children", children);
    }

    private JSONObject buildItem(Object[] o) {
        return JSONUtils.toJSONObject(
                new String[]{"id", "text"},
                new Object[]{o[0], o[1]});
    }
}
