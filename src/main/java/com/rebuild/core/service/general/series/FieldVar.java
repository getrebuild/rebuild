/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 字段值系列
 *
 * @author devezhao
 * @since 07/28/2022
 */
@Slf4j
public class FieldVar extends SeriesVar {

    protected static final String PREFIX = "@";

    private Record record;

    protected FieldVar(String symbols, Record record) {
        super(symbols);
        this.record = record;
    }

    @Override
    public String generate() {
        String field = getSymbols();
        field = field.substring(1);  // Remove `@`
        // v4.3.2
        String defaultValue = null;
        if (field.contains(":")) {
            String[] fs = field.split(":");
            field = fs[0];
            defaultValue = fs.length > 1 ? fs[1] : null;
        }

        if (this.record == null) {
            log.warn("No record spectify ignored");
            return wrapValue(defaultValue);
        }

        if (MetadataHelper.getLastJoinField(record.getEntity(), field) == null) {
            log.warn("Invalid field : {} in {}", field, record.getEntity().getName());
            return wrapValue(defaultValue);
        }

        String[] fieldPath = field.split("\\.");

        Object val = record.getObjectValue(fieldPath[0]);
        if (NullValue.isNull(val)) return wrapValue(defaultValue);

        if (fieldPath.length == 1) {
            val = FieldValueHelper.wrapFieldValue(val, record.getEntity().getField(fieldPath[0]), true);
            return wrapValue(val);
        }

        Object[] o = Application.getQueryFactory()
                .uniqueNoFilter((ID) val, field.substring(field.indexOf(".") + 1));
        if (o == null || o[0] == null) return wrapValue(defaultValue);

        // fix:4.3.2
        Field lastField = MetadataHelper.getLastJoinField(record.getEntity(), field);
        val = FieldValueHelper.wrapFieldValue(val, lastField, true);
        return wrapValue(val == null ? defaultValue : val);
    }

    private String wrapValue(Object val) {
        if (val == null) return StringUtils.EMPTY;
        else return CommonsUtils.maxstr(val.toString(), 20);
    }
}
