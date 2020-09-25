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
 * Record 构建
 *
 * @author devezhao
 * @since 2020/1/17
 */
public class RecordBuilder implements JSONable {

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
     * @param record
     * @return
     */
    public static RecordBuilder builder(ID record) {
        return new RecordBuilder(MetadataHelper.getEntity(record.getEntityCode()), record);
    }

    // --

    final private Entity entity;

    final private JSONObject data = new JSONObject();

    /**
     * @param entity
     * @param record
     */
    private RecordBuilder(Entity entity, ID record) {
        this.entity = entity;

        JSONObject metadata = new JSONObject();
        metadata.put("entity", entity.getName());
        if (record != null) {
            metadata.put("id", record.toLiteral());
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
