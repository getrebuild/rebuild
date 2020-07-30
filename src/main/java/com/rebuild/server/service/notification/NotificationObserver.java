/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.notification;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * @author devezhao
 * @since 11/01/2018
 */
public class NotificationObserver extends OperatingObserver {

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
	public void onAssign(OperatingContext context) {
		final ID related = context.getAfterRecord().getPrimary();
		if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(related);
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID(EntityHelper.OwningUser);
		
		String content = makeMessage(context.getAffected(), related, false);
		content = MessageFormat.format(content, from, context.getAffected().length, getLabel(related));
		Application.getNotifications().send(
				MessageBuilder.createMessage(to, content, Message.TYPE_ASSIGN));
	}
	
	@Override
	public void onShare(OperatingContext context) {
		final ID related = context.getAfterRecord().getID("recordId");
		if (NotificationOnce.didBegin()) {
            NotificationOnce.getMergeSet().add(related);
			return;
		}
		
		ID from = context.getOperator();
		ID to = context.getAfterRecord().getID("shareTo");
		
		String content = makeMessage(context.getAffected(), related, true);
		content = MessageFormat.format(content, from, context.getAffected().length, getLabel(related));
		Application.getNotifications().send(
				MessageBuilder.createMessage(to, content, Message.TYPE_SAHRE));
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
	private String makeMessage(ID[] affected, ID related, boolean shareType) {
		String msg = "@{0} 共享了 {1} 条{2}记录给你";
		if (affected.length > 1) {
			for (ID id : affected) {
				if (id.getEntityCode().intValue() != related.getEntityCode().intValue()) {
					msg = "@{0} 共享了{2}及其关联记录共 {1} 条记录给你";
					break;
				}
			}
			msg += "，包括 @";
			
			String atrs = StringUtils.join(ArrayUtils.subarray(affected, 0, 10), " @");
			msg += atrs;
			if (affected.length > 10) {
				msg += " 等";
			}
		} else {
			msg += " @" + related;
		}
		
		if (!shareType) {
			msg = msg.replace(" 共享", " 分派");
		}
		return msg;
	}
}
