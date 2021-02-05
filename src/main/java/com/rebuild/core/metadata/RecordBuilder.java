/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONable;

/**
 * Record 构建器
 *
 * @author devezhao
 * @since 2020/1/17
 */
public class RecordBuilder implements JSONable {
    private static final long serialVersionUID = -1255623355715103385L;

    /**
     * @param entity
     * @return
     */
    public static RecordBuilder builder(Entity entity) {
        return new RecordBuilder(entity, null);
    }

    /**
     * @param entityCode
     * @return
     */
    public static RecordBuilder builder(int entityCode) {
        return new RecordBuilder(MetadataHelper.getEntity(entityCode), null);
    }

    /**
     * @param recordId
     * @return
     */
    public static RecordBuilder builder(ID recordId) {
        return new RecordBuilder(MetadataHelper.getEntity(recordId.getEntityCode()), recordId);
    }

    // --

    final private Entity entity;

    final private JSONObject data = new JSONObject();

    /**
     * @param entity
     * @param recordId
     */
    private RecordBuilder(Entity entity, ID recordId) {
        this.entity = entity;

        JSONObject metadata = new JSONObject();
        metadata.put("entity", entity.getName());
        if (recordId != null) {
            metadata.put("id", recordId.toLiteral());
        }
        this.data.put(EntityRecordCreator.META_FIELD, metadata);
    }

    /**
     * 添加数据。注意此方法不做任何数据合适转换，需自行处理
     *
     * @param name
     * @param value
     * @return
     */
    public RecordBuilder add(String name, Object value) {
        data.put(name, value);
        return this;
    }

    @Override
    public JSON toJSON() {
        return data;
    }

    /**
     * @param editor
     * @return
     * @see EntityRecordCreator
     */
    public Record build(ID editor) {
        return new EntityRecordCreator(this.entity, this.data, editor).create();
    }
}
