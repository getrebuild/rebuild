/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.NullValue;
import com.rebuild.core.DefinedException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Zixin (RB)
 * @since 2021/01/12
 */
public class RepeatedRecordsException extends DefinedException {
    private static final long serialVersionUID = 8769785498603769556L;

    private final List<Record> repeatedRecords;

    public RepeatedRecordsException(List<Record> repeated) {
        super("There are " + repeated.size() + " repeated records");
        this.repeatedRecords = repeated;
    }

    @Override
    public int getErrorCode() {
        return CODE_RECORDS_REPEATED;
    }

    public List<Record> getRepeatedRecords() {
        return repeatedRecords;
    }

    /**
     * 从重复记录中提取字段名和值，构建具体错误消息
     * 格式如: "客户名称:张三 重复" 或 "客户名称:张三、编号:A001 重复（共 3 条）"
     *
     * @param repeatedRecords
     * @return
     */
    public static String buildRepeatedMessage(List<Record> repeatedRecords) {
        if (repeatedRecords == null || repeatedRecords.isEmpty()) {
            return Language.L("存在重复记录");
        }

        final Record first = repeatedRecords.get(0);
        final Entity entity = first.getEntity();
        final String pkName = entity.getPrimaryField().getName();
        final List<String> parts = new ArrayList<>();

        for (Iterator<String> iter = first.getAvailableFieldIterator(); iter.hasNext(); ) {
            String fieldName = iter.next();
            if (fieldName.equalsIgnoreCase(pkName)) continue;

            Field field = entity.getField(fieldName);
            if (field == null || field.isRepeatable()
                    || MetadataHelper.isCommonsField(field)
                    || EasyMetaFactory.getDisplayType(field) == DisplayType.SERIES) {
                continue;
            }

            Object value = first.getObjectValue(fieldName);
            if (value == null || NullValue.isNull(value)) continue;

            Object displayValue = FieldValueHelper.wrapFieldValue(value, field, true);
            if (displayValue == null) continue;

            String valueStr = displayValue.toString();
            if (valueStr.length() > 30) valueStr = valueStr.substring(0, 30) + "...";

            parts.add(EasyMetaFactory.getLabel(field) + ":" + valueStr);
            if (parts.size() >= 2) break;
        }

        if (parts.isEmpty()) return Language.L("存在重复记录");

        String fieldsStr = StringUtils.join(parts.toArray(), "、");
        if (repeatedRecords.size() > 1) {
            return Language.L("%s 重复 %d 条", fieldsStr, repeatedRecords.size());
        }
        return Language.L("%s 重复", fieldsStr);
    }
}
