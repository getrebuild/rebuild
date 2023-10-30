/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.utils.JSONUtils;

/**
 * 轻量级表单
 * Issue1. 针对自动只读字段无效
 * Issue2. 表单高级控制无效
 *
 * @author devezhao
 * @since 2022/12/28
 * @see FormsBuilder
 */
public class LiteFormBuilder {

    final private ID recordId;
    final private Entity entity;
    final private ID user;

    /**
     * 编辑
     *
     * @param recordId
     * @param user
     */
    public LiteFormBuilder(ID recordId, ID user) {
        this.recordId = recordId;
        this.entity = MetadataHelper.getEntity(recordId.getEntityCode());
        this.user = user;
    }

    /**
     * 新建
     *
     * @param entity
     * @param user
     */
    public LiteFormBuilder(Entity entity, ID user) {
        this.recordId = null;
        this.entity = entity;
        this.user = user;
    }

    /**
     * @param fieldElements
     * @return
     */
    public JSONArray build(JSONArray fieldElements) {
        Record recordData = null;
        if (recordId != null) {
            recordData = FormsBuilder.instance.findRecord(recordId, user, fieldElements);
            if (recordData == null) {
                throw new NoRecordFoundException(recordId, Boolean.TRUE);
            }
        }

        FormsBuilder.instance.buildModelElements(fieldElements, entity, recordData, user, false, false);
        return fieldElements;
    }

    /**
     * @param fields
     * @return
     */
    public JSONArray build(String[] fields) {
        JSONArray fieldElements = new JSONArray();
        for (String field : fields) {
            if (entity.containsField(field)) {
                fieldElements.add(JSONUtils.toJSONObject(
                        new String[] { "field", "colspan" }, new Object[] { field, 4 }));
            }
        }
        return build(fieldElements);
    }
}
