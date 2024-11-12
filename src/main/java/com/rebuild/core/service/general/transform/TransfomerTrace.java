/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.transform;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.general.RecordBuilder;

/**
 * 记录转换跟踪
 *
 * @author Zixin
 * @since 2024/11/12
 */
public class TransfomerTrace {

    /**
     * @param sourceRecordId
     * @param targetRecordId
     * @param configId
     * @param user
     */
    public static void trace(ID sourceRecordId, ID targetRecordId, ID configId, ID user) {
        Record r = RecordBuilder.builder(EntityHelper.TransformTrace)
                .add("sourceRecordId", sourceRecordId)
                .add("targetRecordId", targetRecordId)
                .add("transformConfigId", configId)
                .build(user);
        Application.getCommonsService().create(r);
    }

    /**
     * @param sourceRecordId
     * @param configId
     * @return
     */
    public static ID getLastTargetRecordId(ID sourceRecordId, ID configId) {
        Object[] o = Application.getQueryFactory().createQueryNoFilter(
                "select targetRecordId from TransformTrace where sourceRecordId = ? and configId = ? order by createdOn desc")
                .setParameter(1, sourceRecordId)
                .setParameter(2, configId)
                .unique();
        return o == null ? null : (ID) o[0];
    }

    /**
     * @param sourceRecordId
     * @return returns [targetRecordId, transformConfigId]
     */
    public static ID[] getLastTargetTrace(ID sourceRecordId) {
        Object[] o = Application.getQueryFactory().createQueryNoFilter(
                "select targetRecordId,transformConfigId from TransformTrace where sourceRecordId = ? order by createdOn desc")
                .setParameter(1, sourceRecordId)
                .unique();
        if (o == null) return null;
        return new ID[]{(ID) o[0], (ID) o[1]};
    }
}
