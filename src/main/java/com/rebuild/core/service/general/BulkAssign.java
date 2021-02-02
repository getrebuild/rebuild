/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.notification.NotificationObserver;
import com.rebuild.core.service.notification.NotificationOnce;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 分派
 *
 * @author devezhao
 * @since 09/29/2018
 */
@Slf4j
public class BulkAssign extends BulkOperator {

    public BulkAssign(BulkContext context, GeneralEntityService ges) {
        super(context, ges);
    }

    @Override
    public Integer exec() {
        final ID[] records = prepareRecords();
        this.setTotal(records.length);

        ID firstAssigned = null;
        NotificationOnce.begin();
        for (ID id : records) {
            if (Application.getPrivilegesManager().allowAssign(context.getOpUser(), id)) {
                int a = ges.assign(id, context.getToUser(), context.getCascades());
                if (a > 0) {
                    this.addSucceeded();
                    if (firstAssigned == null) {
                        firstAssigned = id;
                    }
                }
            } else {
                log.warn("No have privileges to ASSIGN : " + context.getOpUser() + " > " + id);
            }
            this.addCompleted();
        }

        // 合并通知发送
        Set<ID> affected = NotificationOnce.end();
        if (firstAssigned != null && !affected.isEmpty()) {
            Record notificationNeeds = EntityHelper.forUpdate(firstAssigned, context.getOpUser());
            notificationNeeds.setID(EntityHelper.OwningUser, context.getToUser());
            // Once notification
            OperatingContext operatingContext = OperatingContext.create(
                    context.getOpUser(), BizzPermission.ASSIGN, null, notificationNeeds, affected.toArray(new ID[0]));
            new NotificationObserver().update(null, operatingContext);
        }

        return getSucceeded();
    }
}
