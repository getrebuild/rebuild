/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.fieldvalue;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/6/5
 */
public class ContentWithFieldVars {

    /**
     * 替换文本中的字段变量
     *
     * @param content
     * @param record
     * @return
     */
    public static String replaceWithRecord(String content, ID record) {
        if (StringUtils.isBlank(content) || record == null) {
            return content;
        }

        Map<String, String> fieldVars = new HashMap<>();
        Entity entity = MetadataHelper.getEntity(record.getEntityCode());
        for (String name : CommonsUtils.matchsVars(content)) {
            if (MetadataHelper.checkAndWarnField(entity, name)) {
                fieldVars.put(name, null);
            }
        }

        if (fieldVars.isEmpty()) {
            return content;
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fieldVars.keySet(), ","), entity.getName(), entity.getPrimaryField().getName());
        Record o = Application.createQueryNoFilter(sql).setParameter(1, record).record();
        if (o != null) {
            for (String field : fieldVars.keySet()) {
                Object value = o.getObjectValue(field);
                value = FieldValueWrapper.instance.wrapFieldValue(value, MetadataHelper.getLastJoinField(entity, field), true);
                if (value != null) {
                    fieldVars.put(field, value.toString());
                }
            }
        }

        for (Map.Entry<String, String> e : fieldVars.entrySet()) {
            content = content.replace("{" + e.getKey() + "}", StringUtils.defaultIfBlank(e.getValue(), StringUtils.EMPTY));
        }
        return content;
    }
}
