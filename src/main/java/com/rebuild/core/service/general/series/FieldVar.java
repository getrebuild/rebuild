/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

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
        if (this.record == null) {
            log.warn("No record spectify ignored");
            return wrapValue(null);
        }

        String fieldPath = getSymbols();
        fieldPath = fieldPath.substring(1);  // Remove `@`

        if (MetadataHelper.getLastJoinField(record.getEntity(), fieldPath) == null) {
            log.warn("Invalid field : {} in {}", fieldPath, record.getEntity().getName());
            return wrapValue(null);
        }

        String[] fields = fieldPath.split("\\.");

        Object val = record.getObjectValue(fields[0]);
        if (NullValue.isNull(val)) return wrapValue(null);

        if (fields.length == 1) {
            val = FieldValueHelper.wrapFieldValue(val, record.getEntity().getField(fields[0]), true);
            return wrapValue(val);
        }

        Object[] o = Application.getQueryFactory()
                .uniqueNoFilter((ID) val, fieldPath.substring(fieldPath.indexOf(".") + 1));
        return wrapValue(o == null ? null : o[0]);
    }

    private String wrapValue(Object val) {
        if (val == null) return StringUtils.EMPTY;
        else return CommonsUtils.maxstr(val.toString(), 20);
    }
}
