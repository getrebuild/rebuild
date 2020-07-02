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

    protected ProjectTaskService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public Record create(Record record) {
        if (record.getPrimary() == null) {
            record.setLong("taskNumber", getNextTaskNumber(record.getID("projectId")));
        }
        return super.create(record);
    }

    /**
     * @param projectId
     * @return
     */
    synchronized private long getNextTaskNumber(ID projectId) {
        Object[] max = Application.createQueryNoFilter(
                "select max(taskNumber) from ProjectTask where projectId = ?")
                .setParameter(1, projectId)
                .unique();
        return (max == null || max[0] == null) ? 1 : ((Long) max[0] + 1);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTask;
    }
}
