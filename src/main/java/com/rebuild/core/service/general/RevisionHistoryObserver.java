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
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;

/**
 * 记录变更历史
 *
 * @author devezhao
 * @since 10/31/2018
 */
public class RevisionHistoryObserver extends OperatingObserver {

    @Override
    protected void updateByAction(OperatingContext ctx) {
        // 激活
        if (RebuildConfiguration.getInt(ConfigurationItem.RevisionHistoryKeepingDays) > 0) {
            super.updateByAction(ctx);
        } else if (ctx.getAction() != ObservableService.DELETE_BEFORE) {
            LOG.warn("RevisionHistory inactivated : " + ctx.getAnyRecord().getPrimary() + " by " + ctx.getOperator());
        }
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

        OperatingContext source = RobotTriggerObserver.getTriggerSource();
        if (source != null) {
            record.setID("channelWith", source.getAnyRecord().getPrimary());
        }

        return record;
    }

    @Override
    protected boolean isAsync() {
        return true;
    }
}