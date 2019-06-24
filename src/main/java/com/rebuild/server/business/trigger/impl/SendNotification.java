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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

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
		
		ID[] toUsers = parseSendTo(content.getJSONArray("sendTo"));
		if (toUsers.length == 0) {
			return;
		}
		
		String message = content.getString("content");
		message = formatMessage(message);
		for (ID user : toUsers) {
			Application.getNotifications().send(new Message(user, message, context.getSourceRecord()));
		}
	}
	
	@Override
	public void prepare(OperatingContext operatingContext) throws TriggerException {
	}
	
	/**
	 * @param sendTo
	 * @return
	 */
	private ID[] parseSendTo(JSONArray tos) {
		final Entity entity = context.getSourceEntity();
		
		Set<ID> bizzs = new HashSet<ID>();
		Set<String> fromFields = new HashSet<>();
		for (Object to : tos) {
			String to2 = (String) to;
			if (ID.isId(to2)) {
				bizzs.add(ID.valueOf(to2));
			} else if (MetadataHelper.getLastJoinField(entity, to2) != null) {
				fromFields.add(to2);
			}
		}
		
		if (!fromFields.isEmpty()) {
			String sql = String.format("select %s from %s where %s = ?", 
					StringUtils.join(fromFields.iterator(), ","), entity.getName(), entity.getPrimaryField().getName());
			Object[] bizzValues = Application.createQueryNoFilter(sql).setParameter(1, context.getSourceRecord()).unique();
			for (Object bizz : bizzValues) {
				if (bizz != null) {
					bizzs.add((ID) bizz);
				}
			}
		}
		
		Set<ID> users = new HashSet<>();
		for (ID bizz : bizzs) {
			if (bizz.getEntityCode() == EntityHelper.User) {
				users.add(bizz);
			} else if (bizz.getEntityCode() == EntityHelper.Department || bizz.getEntityCode() == EntityHelper.Role) {
				Member ms[] = UserHelper.getMembers(bizz);
				for (Member m : ms) {
					users.add((ID) m.getIdentity());
				}
			}
		}
		return users.toArray(new ID[users.size()]);
	}
	
	/**
	 * @param message
	 * @return
	 */
	private String formatMessage(String message) {
		return message;
	}
}
