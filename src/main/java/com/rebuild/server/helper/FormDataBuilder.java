/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2020 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.ExtRecordCreator;
import com.rebuild.server.metadata.MetadataHelper;

/**
 * 表单数据构建
 *
 * @author devezhao
 * @since 2020/1/17
 */
public class FormDataBuilder {

    /**
     * @param entity
     * @return
     */
    public static FormDataBuilder builder(Entity entity) {
        return new FormDataBuilder(entity, null);
    }

    /**
     * @param entityCode
     * @return
     */
    public static FormDataBuilder builder(int entityCode) {
        return new FormDataBuilder(MetadataHelper.getEntity(entityCode), null);
    }

    /**
     * @param record
     * @return
     */
    public static FormDataBuilder builder(ID record) {
        return new FormDataBuilder(MetadataHelper.getEntity(record.getEntityCode()), record);
    }

    // --

    final private Entity entity;

    final private JSONObject data = new JSONObject();

    /**
     * @param entity
     * @param record
     */
    private FormDataBuilder(Entity entity, ID record) {
        this.entity = entity;

        JSONObject metadata = new JSONObject();
        metadata.put("entity", entity.getName());
        if (record != null) {
            metadata.put("id", record.toLiteral());
        }
        this.data.put(ExtRecordCreator.META_FIELD, metadata);
    }

    /**
     * 添加数据。注意此方法不做任何数据合适转换，需自行处理
     *
     * @param name
     * @param value
     * @return
     */
    public FormDataBuilder add(String name, Object value) {
        data.put(name, value);
        return this;
    }

    /**
     * @return
     */
    public JSONObject build() {
        return data;
    }

    /**
     * @param editor
     * @return
     * @see ExtRecordCreator
     */
    public Record buildRecord(ID editor) {
        return new ExtRecordCreator(this.entity, this.build(), editor).create();
    }
}
