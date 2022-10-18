/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;


/**
 * 多引用字段支持
 *
 * @author devezhao
 * @since 2021/11/19
 * @see com.rebuild.core.metadata.easymeta.EasyN2NReference
 */
@Slf4j
public class N2NReferenceSupport {

    /**
     * 获取引用项（仅本实体 N2N 字段有效）
     *
     * @param field
     * @param recordId 主键
     * @return
     */
    public static ID[] items(Field field, ID recordId) {
        if ((int) field.getOwnEntity().getEntityCode() != recordId.getEntityCode()) {
            log.warn("Bad id for found n2n-value : {} > {}", recordId, field);
            return new ID[] { recordId };
        }
        
        Object[][] array = Application.getPersistManagerFactory().createQuery(
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
            Object[] o = Application.getPersistManagerFactory().createQuery(
                    String.format("select %s from %s where %s = ?", field, father.getName(), father.getPrimaryField().getName()))
                    .setParameter(1, fatherRecordId)
                    .unique();

            fatherRecordId = (ID) o[0];
            father = father.getField(field).getReferenceEntity();
        }

        Field lastField = father.getField(paths[paths.length - 1]);
        return items(lastField, fatherRecordId);
    }

    /**
     * 获取谁引用了我 <tt>referenceId</tt>
     *
     * @param field
     * @param referenceId
     * @return
     */
    public static Set<ID> findReferences(Field field, ID referenceId) {
        Object[][] array = Application.createQueryNoFilter(
                "select recordId from NreferenceItem where belongEntity = ? and belongField = ? and referenceId = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .setParameter(3, referenceId)
                .array();

        Set<ID> set = new HashSet<>();
        for (Object[] o : array) set.add((ID) o[0]);
        return set;
    }
}
