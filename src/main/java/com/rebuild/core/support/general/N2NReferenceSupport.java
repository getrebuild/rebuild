/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.springframework.util.Assert;


/**
 * 多引用字段支持
 *
 * @author devezhao
 * @since 2021/11/19
 */
public class N2NReferenceSupport {

    /**
     * 获取引用项（仅本实体 N2N 字段）
     *
     * @param field
     * @param recordId 主键
     * @return
     */
    public static ID[] items(Field field, ID recordId) {
        Object[][] array = Application.createQueryNoFilter(
                        "select referenceId from NreferenceItem where belongField = ? and recordId = ? order by seq")
                .setParameter(1, field.getName())
                .setParameter(2, recordId)
                .array();
        if (array.length == 0) return ID.EMPTY_ID_ARRAY;

        ID[] ids = new ID[array.length];
        for (int i = 0; i < array.length; i++) {
            ids[i] = (ID) array[i][0];
        }
        return ids;
    }

    /**
     * 获取引用项
     *
     * @param fieldPath
     * @param recordId 主键
     * @return
     */
    public static ID[] items(String fieldPath, ID recordId) {
        Entity father = MetadataHelper.getEntity(recordId.getEntityCode());
        ID fatherRecordId = recordId;

        String[] paths = fieldPath.split("\\.");
        for (int i = 0; i < paths.length - 1; i++) {
            String field = paths[i];
            Object[] o = Application.getQueryFactory().uniqueNoFilter(fatherRecordId, field);

            fatherRecordId = (ID) o[0];
            father = father.getField(field).getReferenceEntity();
        }

        Field lastField = father.getField(paths[paths.length - 1]);
        return items(lastField, fatherRecordId);
    }

    /**
     * 补充 N2N 字段真实的值
     *
     * @param record
     * @return
     */
    public static boolean fillN2NValues(Record record) {
        Field[] n2nFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.N2NREFERENCE);
        if (n2nFields.length == 0) return false;

        ID primaryId = record.getPrimary();
        Assert.notNull(primaryId, "Record primary cannot be null");

        boolean filled = false;
        for (Field n2nField : n2nFields) {
            if (record.hasValue(n2nField.getName(), false)) {
                record.setIDArray(n2nField.getName(), items(n2nField, primaryId));
                filled = true;
            }
        }
        return filled;
    }
}
