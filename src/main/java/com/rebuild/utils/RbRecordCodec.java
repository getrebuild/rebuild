/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.utils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.rebuild.server.configuration.portals.FieldValueWrapper;

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
            value = FieldValueWrapper.instance.wrapFieldValue(value, entity.getField(field));
            map.put(field, value);
        }
//        // 添加一个固定的主键字段便于使用
//        if (record.getPrimary() != null) {
//            map.put("__id", record.getPrimary());
//        }

        String text = map.toJSONString();
        out.write(text);
    }
}
