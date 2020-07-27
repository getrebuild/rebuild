/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.configuration.ProjectPlanConfigService;

import java.util.Set;

/**
 * @author devezhao
 * @since 2020/7/2
 * @see com.rebuild.server.service.configuration.ProjectConfigService
 * @see com.rebuild.server.service.configuration.ProjectPlanConfigService
 */
public class ProjectTaskService extends AtUserAwareService {

    // 中值法排序
    private static final int MID_VALUE = 1000;

    protected ProjectTaskService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.ProjectTask;
    }

    @Override
    public Record create(Record record) {
        checkMember(null, record.getID("projectId"));

        record.setLong("taskNumber", getNextTaskNumber(record.getID("projectId")));
        applyFlowStatue(record);
        record.setInt("seq", getNextSeqViaMidValue(record.getID("projectPlanId")));

        record = super.create(record);
        checkAtUserAndNotification(record, record.getString("description"));
        return record;
    }

    @Override
    public Record update(Record record) {
        checkMember(null, record.getPrimary());

        // 自动完成
        int flowStatus = applyFlowStatue(record);

        if (flowStatus == ProjectPlanConfigService.FLOW_STATUS_END) {
            record.setDate("endTime", CalendarUtils.now());
        } else if (record.hasValue("status")) {
            int status = record.getInt("status");
            // 处理完成时间
            if (status == 0) {
                record.setNull("endTime");
                record.setInt("seq", getSeqInStatus(record.getPrimary(), false));
            } else {
                record.setDate("endTime", CalendarUtils.now());
                record.setInt("seq", getSeqInStatus(record.getPrimary(), true));
            }

        } else if (record.hasValue("seq")) {
            int seq = record.getInt("seq");
            if (seq == -1) {
                record.setInt("seq", getSeqInStatus(record.getPrimary(), true));
            }
        }

        record = super.update(record);
        checkAtUserAndNotification(record, record.getString("description"));
        return record;
    }

    @Override
    public int delete(ID taskId) {
        checkMember(null, taskId);

        int d = super.delete(taskId);
        ProjectManager.instance.clean(taskId);
        return d;
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
        Object[] seqMax = Application.createQueryNoFilter(
                "select max(seq) from ProjectTask where projectPlanId = ?")
                .setParameter(1, projectPlanId)
                .unique();
        return (seqMax == null || seqMax[0] == null) ? 0 : ((Integer) seqMax[0] + MID_VALUE);
    }

    /**
     * @param taskId
     * @param desc Use max or min
     * @return
     */
    synchronized
    private int getSeqInStatus(ID taskId, boolean desc) {
        Object[] taskStatus = Application.createQuery(
                "select status,projectPlanId from ProjectTask where taskId = ?")
                .setParameter(1, taskId)
                .unique();
        if (taskStatus == null) return 1;

        Object[] seq = Application.createQuery(
                "select " + (desc ? "max" : "min") + "(seq) from ProjectTask where status = ? and projectPlanId = ?")
                .setParameter(1, taskStatus[0])
                .setParameter(2, taskStatus[1])
                .unique();

        if (desc) return (Integer) seq[0] + MID_VALUE;
        else return (Integer) seq[0] - MID_VALUE;
    }

    /**
     * 自动完成
     * @param newOrUpdate
     */
    private int applyFlowStatue(Record newOrUpdate) {
        if (newOrUpdate.hasValue("projectPlanId")) {
            ConfigEntry c = ProjectManager.instance.getPlanOfProject(
                    newOrUpdate.getID("projectPlanId"), newOrUpdate.getID("projectId"));
            int fs = c.getInteger("flowStatus");

            if (fs == ProjectPlanConfigService.FLOW_STATUS_END) {
                newOrUpdate.setInt("status", 1);
            } else if (fs == ProjectPlanConfigService.FLOW_STATUS_PROCESSING) {
                newOrUpdate.setDate("startTime", CalendarUtils.now());
            }
            return fs;
        }
        return -1;
    }

    /**
     * @param user
     * @param taskOrProject
     * @return
     */
    private boolean checkMember(ID user, ID taskOrProject) {
        if (user == null) user = Application.getCurrentUser();

        ConfigEntry c = taskOrProject.getEntityCode() == EntityHelper.ProjectTask
                ? ProjectManager.instance.getProjectByTask(taskOrProject, null)
                : ProjectManager.instance.getProject(taskOrProject, null);
        if (c != null && c.get("members", Set.class).contains(user)) return true;

        throw new DataSpecificationException("非项目成员禁止编辑");
    }
}
