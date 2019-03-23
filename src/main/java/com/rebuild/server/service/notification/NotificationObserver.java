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

package com.rebuild.server.service.notification;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;
import com.rebuild.server.service.base.BulkOperatorTx;

import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 11/01/2018
 */
public class NotificationObserver extends OperatingObserver {
	
	@Override
	public void onAssign(OperatingContext context) {
		if (BulkOperatorTx.isInTx()) {
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);
		ID related = context.getAnyRecord().getPrimary();
		
		String text = "@%s 分派了 1 条%s记录给你。@%s";
		text = String.format(text, from, getLabel(related), related);
		
		Message message = new Message(from, to, text, related);
		Application.getNotifications().send(message);
	}
	
	@Override
	public void onShare(OperatingContext context) {
		if (BulkOperatorTx.isInTx()) {
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID("shareTo");
		ID related = context.getAfterRecord().getID("recordId");
		
		String text = "@%s 共享了 1 条%s记录给你。@%s";
		text = String.format(text, from, getLabel(related), related);
		
		Message message = new Message(from, to, text, related);
		Application.getNotifications().send(message);
	}
	
	private String getLabel(ID rid) {
		return EasyMeta.valueOf(rid.getEntityCode()).getLabel();
	}
}
