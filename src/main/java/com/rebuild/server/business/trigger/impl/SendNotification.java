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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
public class SendNotification implements TriggerAction {

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
		for (ID user : toUsers) {
			Message m = MessageBuilder.createMessage(user, message);
			Application.getNotifications().send(m);
		}
	}
	
	@Override
	public void prepare(OperatingContext operatingContext) throws TriggerException {
		// Nothings
	}

	/**
	 * @param message
	 * @param recordId
	 * @return
	 */
	private String formatMessage(String message, ID recordId) {
		// TODO 处理变量
		return message + " @" + recordId;
	}

	@Override
	public boolean useAsync() {
		return true;
	}
}
