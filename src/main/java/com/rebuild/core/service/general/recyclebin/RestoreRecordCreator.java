/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author devezhao
 * @since 2019/8/21
 */
public class RestoreRecordCreator extends JsonRecordCreator {

    private static final Logger LOG = LoggerFactory.getLogger(RestoreRecordCreator.class);

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

            setFieldValue(field, value == null ? null : value.toString(), record);
        }
        return record;
    }
}
