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

package com.rebuild.server.service.notification;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.MarkdownUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/12
 */
public class MessageBuilder {
	
	/**
	 * @param toUser
	 * @param message
	 * @return
	 */
	public static Message createMessage(ID toUser, String message) {
		return new Message(null, toUser, message, null, Message.TYPE_DEFAULT);
	}
	
	/**
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @return
	 */
	public static Message createMessage(ID toUser, String message, ID relatedRecord) {
		return new Message(null, toUser, message, relatedRecord, Message.TYPE_DEFAULT);
	}
	
	/**
	 * @param fromUser
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @return
	 */
	public static Message createMessage(ID fromUser, ID toUser, String message) {
		return new Message(fromUser, toUser, message, null, Message.TYPE_DEFAULT);
	}
	
	/**
	 * @param fromUser
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @return
	 */
	public static Message createMessage(ID fromUser, ID toUser, String message, ID relatedRecord) {
		return new Message(fromUser, toUser, message, relatedRecord, Message.TYPE_DEFAULT);
	}
	
	/**
	 * @param fromUser
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @return
	 */
	public static Message createApprovalMessage(ID toUser, String message) {
		return new Message(null, toUser, message, null, Message.TYPE_APPROVAL);
	}
	
	/**
	 * @param fromUser
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @return
	 */
	public static Message createApprovalMessage(ID fromUser, ID toUser, String message) {
		return new Message(fromUser, toUser, message, null, Message.TYPE_APPROVAL);
	}

	private static final Pattern AT_PATTERN = Pattern.compile("(\\@[0-9a-z\\-]{20})");
	/**
	 * 格式化通知消息为 HTML，支持 MD 语法
	 * 
	 * @param message
	 * @return
	 * @see MarkdownUtils
	 */
	public static String toHTML(String message) {
		// 匹配 `@ID`
		Matcher atMatcher = AT_PATTERN.matcher(message);
		while (atMatcher.find()) {
			String atId = atMatcher.group();
			String atText = parseAtId(atId.substring(1));
			if (atText != null && !atText.equals(atId)) {
				message = message.replace(atId, atText);
			}
		}
		
		message = MarkdownUtils.parse(message);
		return message;
	}
	
	/**
	 * @param atId
	 * @return
	 */
	private static String parseAtId(String atId) {
		if (!ID.isId(atId)) {
			return atId;
		}
		
		ID thatId = ID.valueOf(atId);
		if (thatId.getEntityCode() == EntityHelper.User) {
			if (Application.getUserStore().exists(thatId)) {
				return Application.getUserStore().getUser(thatId).getFullName();
			} else {
				return "[无效用户]";
			}
		}
		
		Entity entity = MetadataHelper.getEntity(thatId.getEntityCode());
		String recordLabel = null;
		try {
			recordLabel = FieldValueWrapper.getLabel(thatId);
		} catch (NoRecordFoundException ex) {
			recordLabel = "[无效记录]";
		}
		
		String aLink = AppUtils.getContextPath() + MessageFormat.format("/app/{0}/list#!/View/{0}/{1}", entity.getName(), thatId);
		return String.format("[%s](%s)", recordLabel, aLink);
	}
}
