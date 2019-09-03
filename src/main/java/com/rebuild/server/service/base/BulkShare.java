/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.notification.NotificationObserver;

import java.util.Set;

/**
 * 共享
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class BulkShare extends BulkOperator {

	public BulkShare(BulkContext context, GeneralEntityService ges) {
		super(context, ges);
	}

	@Override
	public Integer exec() {
		ID[] records = prepareRecords();
		this.setTotal(records.length);
		
		int shared = 0;
		ID firstShared = null;
		BulkOperatorTx.begin();
		for (ID id : records) {
			if (Application.getSecurityManager().allowedS(context.getOpUser(), id)) {
				int a = ges.share(id, context.getToUser(), context.getCascades());
				if (a > 0) {
					shared += a;
					if (firstShared == null) {
						firstShared = id;
					}
				}
			} else {
				LOG.warn("No have privileges to SHARE : " + context.getOpUser() + " > " + id);
			}
			this.addCompleted();
		}
		
		Set<ID> affected = BulkOperatorTx.getInTxSet();
		BulkOperatorTx.end();
		
		if (firstShared != null && !affected.isEmpty()) {
			Record notificationNeeds = EntityHelper.forNew(EntityHelper.ShareAccess, context.getOpUser());
			notificationNeeds.setID("shareTo", context.getToUser());
			notificationNeeds.setID("recordId", firstShared);
			// Once notification
			OperatingContext operatingContext = OperatingContext.create(
					context.getOpUser(), BizzPermission.SHARE, null, notificationNeeds, affected.toArray(new ID[0]));
			new NotificationObserver().update(null, operatingContext);
		}
		
		this.completedAfter();
		return shared;
	}
}
