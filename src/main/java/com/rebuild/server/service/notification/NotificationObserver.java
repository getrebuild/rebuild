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

import java.text.MessageFormat;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

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
		final ID related = context.getAfterRecord().getPrimary();
		if (BulkOperatorTx.isInTx()) {
			BulkOperatorTx.getInTxSet().add(related);
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);
		
		String msg = makeMsg(context.getAffected(), related, false);
		msg = MessageFormat.format(msg, from, context.getAffected().length, getLabel(related));
		
		Message message = new Message(from, to, msg, related);
		Application.getNotifications().send(message);
	}
	
	@Override
	public void onShare(OperatingContext context) {
		final ID related = context.getAfterRecord().getID("recordId");
		if (BulkOperatorTx.isInTx()) {
			BulkOperatorTx.getInTxSet().add(related);
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID("shareTo");
		
		String msg = makeMsg(context.getAffected(), related, true);
		msg = MessageFormat.format(msg, from, context.getAffected().length, getLabel(related));
		
		Message message = new Message(from, to, msg, related);
		Application.getNotifications().send(message);
	}
	
	/**
	 * @param id
	 * @return
	 */
	private String getLabel(ID id) {
		return EasyMeta.valueOf(id.getEntityCode()).getLabel();
	}
	
	/**
	 * @param affected
	 * @param related
	 * @param shareType
	 * @return
	 */
	private String makeMsg(ID affected[], ID related, boolean shareType) {
		String msg = "@{0} 共享了 {1} 条{2}记录给你，包括 @";
		if (affected.length > 1) {
			for (ID id : affected) {
				if (id.getEntityCode().intValue() != related.getEntityCode().intValue()) {
					msg = "@{0} 共享了{2}及其关联记录共 {1} 条记录给你，包括 @";
					break;
				}
			}
			
			String atrs = StringUtils.join(ArrayUtils.subarray(affected, 0, 10), " @");
			msg += atrs;
			if (affected.length > 10) {
				msg += " 等";
			}
		} else {
			msg += related;
		}
		
		if (!shareType) {
			msg = msg.replace(" 共享", " 分派");
		}
		return msg;
	}
}
