/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.trigger.impl;

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

    // 内部消息
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

		String message = content.getString("content");
		message = formatMessage(message, context.getSourceRecord());

		final int type = content.getIntValue("type");
		final String title = StringUtils.defaultIfBlank(content.getString("title"), "你有一条新通知");

		for (ID user : toUsers) {
		    if (type == TYPE_MAIL) {
		        if (!SMSender.availableMail()) break;

		        String emailAddr = Application.getUserStore().getUser(user).getEmail();
		        if (emailAddr != null) {
		            SMSender.sendMail(emailAddr, title, message);
                }

            } else if (type == TYPE_SMS) {
		        // TODO 发送短信（暂无手机字段）

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
