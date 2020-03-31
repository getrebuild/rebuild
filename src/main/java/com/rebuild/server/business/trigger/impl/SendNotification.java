/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
public class SendNotification implements TriggerAction {

	private static final Log LOG = LogFactory.getLog(SendNotification.class);

    // 通知
    @SuppressWarnings("unused")
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
	public boolean isUsableSourceEntity(int entityCode) {
		return true;
	}

	@Override
	public void execute(OperatingContext operatingContext) {
		final JSONObject content = (JSONObject) context.getActionContent();
		
		JSONArray sendTo = content.getJSONArray("sendTo");
		List<String> sendToList = new ArrayList<>();
		for (Object o : sendTo) {
			sendToList.add((String) o);
		}
		Set<ID> toUsers = UserHelper.parseUsers(sendToList, context.getSourceRecord());
		if (toUsers.isEmpty()) {
			return;
		}

		final int type = content.getIntValue("type");
		if (type == TYPE_MAIL && !SMSender.availableMail()) {
			LOG.warn("Could not send because email-service is unavailable");
		} else if (type == TYPE_SMS && !SMSender.availableSMS()) {
			LOG.warn("Could not send because sms-service is unavailable");
		}

		String message = content.getString("content");
		message = formatMessage(message, context.getSourceRecord());
		// for email
		String subject = StringUtils.defaultIfBlank(content.getString("title"), "你有一条新通知");

		for (ID user : toUsers) {
		    if (type == TYPE_MAIL) {
				String emailAddr = Application.getUserStore().getUser(user).getEmail();
		        if (emailAddr != null) {
					SMSender.sendMail(emailAddr, subject, message);
                }

            } else if (type == TYPE_SMS) {
				String mobile = Application.getUserStore().getUser(user).getWorkphone();
		    	if (mobile != null && RegexUtils.isCNMobile(mobile)) {
					SMSender.sendSMS(mobile, message);
				}

            } else {
    			Message m = MessageBuilder.createMessage(user, message, context.getSourceRecord());
	    		Application.getNotifications().send(m);

            }
		}
	}
	
	@Override
	public void prepare(OperatingContext operatingContext) throws TriggerException {
		// NOOP
	}

    private static final Pattern PATT_FIELD = Pattern.compile("\\{([0-9a-zA-Z._]+)}");
	/**
	 * @param message
	 * @param recordId
	 * @return
	 */
	protected String formatMessage(String message, ID recordId) {
        Map<String, String> vars = null;
	    if (recordId != null) {
	        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
            vars = new HashMap<>();

            Matcher m = PATT_FIELD.matcher(message);
            while (m.find()) {
                String field = m.group(1);
                if (MetadataHelper.getLastJoinField(entity, field) == null) {
                    continue;
                }
                vars.put(field, null);
            }

            if (!vars.isEmpty()) {
                String sql = String.format("select %s from %s where %s = ?",
                        StringUtils.join(vars.keySet(), ","), entity.getName(), entity.getPrimaryField().getName());

                Record o = Application.createQueryNoFilter(sql)
                        .setParameter(1, recordId)
                        .record();
                if (o != null) {
                    for (String field : vars.keySet()) {
                        Object value = o.getObjectValue(field);
                        value = FieldValueWrapper.instance.wrapFieldValue(
                                value, MetadataHelper.getLastJoinField(entity, field), true);
                        if (value != null) {
                            vars.put(field, value.toString());
                        }
                    }
                }
            }
        }
	    
	    if (vars != null) {
	        for (Map.Entry<String, String> e : vars.entrySet()) {
	            message = message.replaceAll(
	                    "\\{" + e.getKey() + "}", StringUtils.defaultIfBlank(e.getValue(), StringUtils.EMPTY));
            }
        }
        return message;
	}

	@Override
	public boolean useAsync() {
		return true;
	}
}
