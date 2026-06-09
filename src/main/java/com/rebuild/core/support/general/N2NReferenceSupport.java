/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.query.QueryHelper;
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
        Object[] last = getLastObject(fieldPath, recordId);
        return items((Field) last[0], (ID) last[1]);
    }

    /**
     * @param fieldPath
     * @param recordId
     * @return
     */
    protected static Object[] getLastObject(String fieldPath, ID recordId) {
        Entity father = MetadataHelper.getEntity(recordId.getEntityCode());
        ID fatherRecordId = recordId;

        String[] paths = fieldPath.split("\\.");
        for (int i = 0; i < paths.length - 1; i++) {
            String field = paths[i];
            String sql = String.format("select %s from %s where %s = ?", field, father.getName(), father.getPrimaryField().getName());
            Object[] o = Application.getPersistManagerFactory().createQuery(sql)
                    .setParameter(1, fatherRecordId)
                    .unique();

            fatherRecordId = (ID) o[0];
            father = father.getField(field).getReferenceEntity();
        }

        Field lastField = father.getField(paths[paths.length - 1]);
        return new Object[] { lastField, fatherRecordId };
    }

    /**
     * 获取谁引用了我 `referenceId`
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

    /**
     * 获取 N2N 点连接字段值
     *
     * @param fieldPath N2N, N2N.F, F.N2N, F.N2N.F
     * @param recordId
     * @return
     * @see QueryHelper#queryFieldValue(ID, String, boolean)
     */
    public static Object[] getN2NValueByMixPath(String fieldPath, ID recordId) {
        return QueryHelper.queryFieldValue(recordId, fieldPath,  true);
    }

    /**
     * 是否是 N2N 混合点连接，例如 `duokehu.AccountName`
     *
     * @param fieldPath
     * @param entity
     * @return
     * @see #getN2NValueByMixPath(String, ID)
     */
    public static boolean isN2NMixPath(String fieldPath, Entity entity) {
        String[] fields = fieldPath.split("\\.");
        if (fields.length < 2) return false;
        
        try {
            // N2N.F
            Field firstField = entity.getField(fields[0]);
            if (firstField.getType() == FieldType.REFERENCE_LIST) return true;

            // F.N2N
            Field secondField = firstField.getReferenceEntity().getField(fields[1]);
            return secondField.getType() == FieldType.REFERENCE_LIST;

        } catch (MissingMetaExcetion ex) {
            throw new MissingMetaExcetion(fieldPath, entity.getName());
        }
    }
}
