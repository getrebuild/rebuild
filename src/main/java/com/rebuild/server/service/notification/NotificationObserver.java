/*
rebuild - Building your system freely.
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
import com.rebuild.server.service.OperateContext;
import com.rebuild.server.service.OperateObserver;

import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 11/01/2018
 */
public class NotificationObserver extends OperateObserver {
	
	@Override
	public void onAssign(OperateContext context) {
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);
		
		ID relatedRecordId = context.getRecordId();
		String text = "@%s 分派了 1 条%s记录给你";
		text = String.format(text, from, EasyMeta.valueOf(relatedRecordId.getEntityCode()).getLabel());
		
		Message message = new Message(from, to, text, relatedRecordId);
		Application.getNotifications().send(message);
	}
	
	@Override
	public void onSahre(OperateContext context) {
		ID from = context.getOperator();
		String to = context.getAfterRecord().getString("shareTo");
		
		ID relatedRecordId = context.getRecordId();
		String text = "@%s 共享了 1 条%s记录给你";
		text = String.format(text, from, EasyMeta.valueOf(relatedRecordId.getEntityCode()).getLabel());
		
		Message message = new Message(from, ID.valueOf(to), text, relatedRecordId);
		Application.getNotifications().send(message);
	}
}
