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

package com.rebuild.server.business.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * @author devezhao
 * @since 2019/8/21
 */
public class RestoreRecordCreator extends JsonRecordCreator {

    private static final Log LOG = LogFactory.getLog(RestoreRecordCreator.class);

    public RestoreRecordCreator(Entity entity, JSONObject source) {
        super(entity, source);
    }

    @Override
    public Record create(boolean ignoreNullValue) {
        Record record = new StandardRecord(entity, null);

        for (Map.Entry<String, Object> e : source.entrySet()) {
            String fileName = e.getKey();
            if (!entity.containsField(fileName)) {
                LOG.warn("Unable found field [ " + entity.getName() + '#' + fileName + " ], will ignore");
                continue;
            }

            Field field = entity.getField(fileName);
            Object value = e.getValue();
            if (ignoreNullValue && (value == null || StringUtils.isEmpty(value.toString()))) {
                continue;
            }
            setValue(field, value == null ? null : value.toString(), record);
        }
        return record;
    }
}
