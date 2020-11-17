/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyMeta;

/**
 * 获取实体列表
 *
 * @author devezhao
 * @since 2020/5/14
 */
public class EntityList extends BaseApi {

    @Override
    protected String getApiName() {
        return "metadata/entities";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        JSONArray array = new JSONArray();
        for (Entity e : MetadataSorter.sortEntities()) {
            array.add(buildEntity(e));
        }
        return formatSuccess(array);
    }

    private JSONObject buildEntity(Entity entity) {
        JSONObject o = new JSONObject();
        o.put("type_code", entity.getEntityCode());
        o.put("entity_name", entity.getName());
        o.put("entity_label", EasyMetaFactory.getLabel(entity));

        o.put("creatable", entity.isCreatable());
        o.put("updatable", entity.isUpdatable());
        o.put("queryable", entity.isQueryable());
        o.put("deletable", entity.isDeletable());

        if (entity.getMainEntity() != null) {
            o.put("main_entity", entity.getMainEntity().getName());
        }
        if (entity.getDetailEntity() != null) {
            o.put("detail_entity", entity.getDetailEntity().getName());
        }
        return o;
    }
}
