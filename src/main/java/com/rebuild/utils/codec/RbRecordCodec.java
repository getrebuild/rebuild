/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.codec;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.rebuild.core.support.general.FieldValueWrapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * {@link Record} JSON 编码
 *
 * @author devezhao
 * @since 2019/9/3
 */
public class RbRecordCodec implements ObjectSerializer {

    public final static RbRecordCodec instance = new RbRecordCodec();

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object == null) {
            out.writeNull();
            return;
        }

        Record record = (Record) object;
        Entity entity = record.getEntity();
        JSONObject map = new JSONObject();
        for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
            String field = iter.next();
            Object value = record.getObjectValue(field);
            value = FieldValueWrapper.instance.wrapFieldValue(value, entity.getField(field), false);
            map.put(field, value);
        }
        out.write(map.toJSONString());
    }
}
