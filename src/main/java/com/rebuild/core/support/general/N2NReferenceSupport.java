/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
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
     * 获取引用项
     *
     * @param field
     * @param recordId
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
