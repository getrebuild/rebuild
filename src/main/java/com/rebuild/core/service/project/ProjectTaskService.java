/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.Language;
import org.springframework.stereotype.Service;

/**
 * @author devezhao
 * @see ProjectConfigService
 * @see ProjectPlanConfigService
 * @since 2020/7/2
 */
@Service
public class ProjectTaskService extends BaseTaskService {

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
        final ID user = UserContextHolder.getUser();
        checkInMembers(user, record.getID("projectId"));

        record.setLong("taskNumber", getNextTaskNumber(record.getID("projectId")));
        applyFlowStatue(record);
        record.setInt("seq", getNextSeqViaMidValue(record.getID("projectPlanId")));

        record = super.create(record);

        if (record.hasValue("executor", false)) {
            sendNotification(record.getPrimary());
        }
        return record;
    }

    @Override
    public Record update(Record record) {
        final ID user = UserContextHolder.getUser();
        checkInMembers(user, record.getPrimary());

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

        if (record.hasValue("executor", false)) {
            sendNotification(record.getPrimary());
        }
        return record;
    }

    @Override
    public int delete(ID taskId) {
        final ID user = UserContextHolder.getUser();
        if (!ProjectHelper.isManageable(taskId, user)) throw new PrivilegesException("DELETETASK");

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
     * @param desc   Use max or min
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
     *
     * @param newOrUpdate
     */
    private int applyFlowStatue(Record newOrUpdate) {
        if (newOrUpdate.hasValue("projectPlanId")) {
            ConfigBean c = ProjectManager.instance.getPlanOfProject(
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
     * @param taskId
     */
    private void sendNotification(ID taskId) {
        Object[] task = Application.getQueryFactory().uniqueNoFilter(taskId, "executor", "taskName");
        String msg = Language.L("MsgNewProjectTaskToYou") + " \n> " + task[1];
        Application.getNotifications().send(
                MessageBuilder.createMessage((ID) task[0], msg, Message.TYPE_PROJECT, taskId));
    }
}
