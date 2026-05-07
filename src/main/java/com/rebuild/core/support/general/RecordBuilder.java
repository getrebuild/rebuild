/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;

import java.util.HashMap;
import java.util.Map;

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
    final private ID recordId;

    final private Map<String, Object> data = new HashMap<>();

    /**
     * @param entity
     * @param recordId
     */
    private RecordBuilder(Entity entity, ID recordId) {
        this.entity = entity;
        this.recordId = recordId;
    }

    /**
     * 添加数据（注意此方法不做任何数据格式转换，需自行处理）
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
        JSONObject md = JSONUtils.toJSONObject("entity", entity.getName());
        if (recordId != null) md.put("id", recordId.toLiteral());

        JSONObject be = (JSONObject) JSON.toJSON(data);
        be.put(EntityRecordCreator.META_FIELD, md);
        return be;
    }

    /**
     * 构建
     *
     * @param editor
     * @return
     * @see EntityRecordCreator
     */
    public Record build(ID editor) {
        Record record = recordId == null
                ? EntityHelper.forNew(entity.getEntityCode(), editor)
                : EntityHelper.forUpdate(recordId, editor);
        for (Map.Entry<String, Object> e : data.entrySet()) {
            record.setObjectValue(e.getKey(), e.getValue());
        }

        AutoFillinManager.instance.fillinRecord(record);
        return record;
    }

    /**
     * 保存
     *
     * @param editor
     * @return
     * @see com.rebuild.core.service.CommonsService#createOrUpdate(Record)
     */
    public Record save(ID editor) {
        Record r = build(editor);
        Application.getCommonsService().createOrUpdate(r);
        return r;
    }
}
