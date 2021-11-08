/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.recyclebin.RecycleBinCleanerJob;
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 记录变更历史
 *
 * @author devezhao
 * @since 10/31/2018
 */
@Slf4j
public class RevisionHistoryObserver extends OperatingObserver {

    @Override
    protected void updateByAction(OperatingContext ctx) {
        if (isIgnore(ctx)) return;

        // 激活
        if (RecycleBinCleanerJob.isEnableRevisionHistory()) {
            super.updateByAction(ctx);
        } else if (ctx.getAction() != ObservableService.DELETE_BEFORE) {
            log.warn("RevisionHistory inactivated! {} {} by {}",
                    ctx.getAction().getName().toLowerCase(), ctx.getAnyRecord().getPrimary(), ctx.getOperator());
        }
    }

    /**
     * 忽略的
     *
     * @param ctx
     * @return
     */
    private boolean isIgnore(OperatingContext ctx) {
        int entity = ctx.getAnyRecord().getEntity().getEntityCode();
        return entity == EntityHelper.FeedsComment || entity == EntityHelper.ProjectTaskComment;
    }

    @Override
    public void onCreate(OperatingContext context) {
        Record revision = newRevision(context, false);
        Application.getCommonsService().create(revision);
    }

    @Override
    public void onUpdate(OperatingContext context) {
        Record revision = newRevision(context, true);
        Application.getCommonsService().create(revision);
    }

    @Override
    public void onDelete(OperatingContext context) {
        Record revision = newRevision(context, false);
        Application.getCommonsService().create(revision);
    }

    @Override
    public void onAssign(OperatingContext context) {
        Record revision = newRevision(context, true);
        Application.getCommonsService().create(revision);
    }

    @Override
    public void onShare(OperatingContext context) {
        Record revision = newRevision(context, false);
        ID recordId = context.getAfterRecord().getID("recordId");
        revision.setID("recordId", recordId);
        revision.setString("belongEntity", MetadataHelper.getEntityName(recordId));
        Application.getCommonsService().create(revision);
    }

    @Override
    public void onUnshare(OperatingContext context) {
        Record revision = newRevision(context, false);
        ID recordId = context.getBeforeRecord().getID("recordId");
        revision.setID("recordId", recordId);
        revision.setString("belongEntity", MetadataHelper.getEntityName(recordId));
        Application.getCommonsService().create(revision);
    }

    /**
     * @param context
     * @param mergeChange
     * @return
     */
    private Record newRevision(OperatingContext context, boolean mergeChange) {
        ID recordId = context.getAnyRecord().getPrimary();
        Record record = EntityHelper.forNew(EntityHelper.RevisionHistory, UserService.SYSTEM_USER);
        record.setString("belongEntity", MetadataHelper.getEntityName(recordId));
        record.setID("recordId", recordId);
        record.setInt("revisionType", context.getAction().getMask());
        record.setID("revisionBy", context.getOperator());
        record.setDate("revisionOn", CalendarUtils.now());

        if (mergeChange) {
            Record before = context.getBeforeRecord();
            Record after = context.getAfterRecord();
            JSON revisionContent = new RecordDifference(before).merge(after);
            record.setString("revisionContent", revisionContent.toJSONString());
        } else {
            record.setString("revisionContent", JSONUtils.EMPTY_ARRAY_STR);
        }

        OperatingContext triggerSource = RobotTriggerObserver.getTriggerSource();
        if (triggerSource != null) {
            record.setID("channelWith", triggerSource.getAnyRecord().getPrimary());
        }

        if (context.getOperationIp() != null) {
            record.setString("ipAddr", context.getOperationIp());
        }

        return record;
    }

    // TODO 异步无法获知是否关联操作
    @Override
    protected boolean isAsync() {
        return false;
    }
}