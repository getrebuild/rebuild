/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.notification.NotificationObserver;
import com.rebuild.server.service.notification.NotificationOnce;

import java.util.Set;

/**
 * 分派
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class BulkAssign extends BulkOperator {

	protected BulkAssign(BulkContext context, GeneralEntityService ges) {
		super(context, ges);
	}

	@Override
	protected Integer exec() {
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
				LOG.warn("No have privileges to ASSIGN : " + context.getOpUser() + " > " + id);
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
