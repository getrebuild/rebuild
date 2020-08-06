/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.fieldvalue.ContentWithFieldVars;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
public class SendNotification implements TriggerAction {

	private static final Log LOG = LogFactory.getLog(SendNotification.class);

    // 通知
    private static final int TYPE_NOTIFICATION = 1;
    // 邮件
    private static final int TYPE_MAIL = 2;
    // 短信
    private static final int TYPE_SMS = 3;

	final private ActionContext context;

	public SendNotification(ActionContext context) {
		this.context = context;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.SENDNOTIFICATION;
	}

	@Override
	public void execute(OperatingContext operatingContext) {
		ThreadPool.exec(() -> {
			try {
				// 等待事物完成
				ThreadPool.waitFor(3000);

				executeAsync();
			} catch (Exception ex) {
				LOG.error(null, ex);
			}
		});
	}

	/**
	 */
	private void executeAsync() {
		final JSONObject content = (JSONObject) context.getActionContent();
		
		Set<ID> toUsers = UserHelper.parseUsers(content.getJSONArray("sendTo"), context.getSourceRecord());
		if (toUsers.isEmpty()) {
			return;
		}

		final int type = content.getIntValue("type");
		if (type == TYPE_MAIL && !SMSender.availableMail()) {
			LOG.warn("Could not send because email-service is unavailable");
			return;
		} else if (type == TYPE_SMS && !SMSender.availableSMS()) {
			LOG.warn("Could not send because sms-service is unavailable");
			return;
		}

		String message = content.getString("content");
		message = ContentWithFieldVars.replaceWithRecord(message, context.getSourceRecord());

		String emailSubject = StringUtils.defaultIfBlank(content.getString("title"), "你有一条新通知");

		for (ID user : toUsers) {
		    if (type == TYPE_MAIL) {
				String emailAddr = Application.getUserStore().getUser(user).getEmail();
		        if (emailAddr != null) {
					SMSender.sendMail(emailAddr, emailSubject, message);
                }

            } else if (type == TYPE_SMS) {
				String mobile = Application.getUserStore().getUser(user).getWorkphone();
		    	if (RegexUtils.isCNMobile(mobile)) {
					SMSender.sendSMS(mobile, message);
				}

            } else {  // TYPE_NOTIFICATION
    			Message m = MessageBuilder.createMessage(user, message, context.getSourceRecord());
	    		Application.getNotifications().send(m);
            }
		}
	}
}
