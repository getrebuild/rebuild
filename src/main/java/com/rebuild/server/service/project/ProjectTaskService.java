/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseService;

/**
 * @author devezhao
 * @since 2020/7/2
 */
public class ProjectTaskService extends BaseService {

    // 中值法排序
    private static final int MID_VALUE = 1000;

    protected ProjectTaskService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        record.setLong("taskNumber", getNextTaskNumber(record.getID("projectId")));
        record.setInt("seq", getNextSeqViaMidValue(record.getID("projectPlanId")));
        return super.create(record);
    }

    /**
     * @param projectId
     * @return
     */
    synchronized
    private long getNextTaskNumber(ID projectId) {
        Object[] max = Application.createQueryNoFilter(
                "select max(taskNumber) from ProjectTask where projectId = ?")
                .setParameter(1, projectId)
                .unique();
        return (max == null || max[0] == null) ? 1 : ((Long) max[0] + 1);
    }

    /**
     * @param projectPlanId
     * @return
     */
    synchronized
    private int getNextSeqViaMidValue(ID projectPlanId) {
        Object[] max = Application.createQueryNoFilter(
                "select max(seq) from ProjectTask where projectPlanId = ?")
                .setParameter(1, projectPlanId)
                .unique();
        return (max == null || max[0] == null) ? 0 : ((Integer) max[0] + MID_VALUE);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTask;
    }
}
