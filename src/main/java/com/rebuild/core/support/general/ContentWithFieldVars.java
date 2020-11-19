/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/6/5
 */
public class ContentWithFieldVars {

    private static final Pattern PATT_VAR = Pattern.compile("\\{([0-9a-zA-Z._]+)}");

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

        Entity entity = MetadataHelper.getEntity(record.getEntityCode());
        // 主键
        content = content.replace("{ID}", String.format("{%s}", entity.getPrimaryField().getName()));

        Map<String, String> fieldVars = new HashMap<>();
        for (String field : matchsVars(content)) {
            if (MetadataHelper.getLastJoinField(entity, field) != null) {
                fieldVars.put(field, null);
            }
        }

        if (fieldVars.isEmpty()) return content;

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(fieldVars.keySet(), ","), entity.getName(), entity.getPrimaryField().getName());
        Record o = Application.createQueryNoFilter(sql).setParameter(1, record).record();
        if (o != null) {
            for (String field : fieldVars.keySet()) {
                Object value = o.getObjectValue(field);
                value = FieldValueHelper.wrapFieldValue(value, MetadataHelper.getLastJoinField(entity, field), true);
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


    /**
     * 提取内容中的变量 {xxx}
     *
     * @param content
     * @return
     */
    public static Set<String> matchsVars(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptySet();
        }

        Set<String> vars = new HashSet<>();
        Matcher m = PATT_VAR.matcher(content);
        while (m.find()) {
            String varName = m.group(1);
            if (StringUtils.isNotBlank(varName)) vars.add(varName);
        }
        return vars;
    }
}
