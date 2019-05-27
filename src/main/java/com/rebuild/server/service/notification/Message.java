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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rebuild.server.Application;
import com.rebuild.server.configuration.base.FieldValueWrapper;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.MarkdownUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 通知消息
 * 
 * @author devezhao
 * @since 10/17/2018
 */
public class Message {

	private ID fromUser;
	private ID toUser;
	private String message;
	
	private ID relatedRecord;

	public Message(ID toUser, String message, ID relatedRecord) {
		this(UserService.SYSTEM_USER, toUser, message, relatedRecord);
	}

	public Message(ID fromUser, ID toUser, String message, ID relatedRecord) {
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.message = message;
		this.relatedRecord = relatedRecord;
	}

	public ID getFromUser() {
		return fromUser;
	}

	public ID getToUser() {
		return toUser;
	}
	
	public ID getRelatedRecord() {
		return relatedRecord;
	}

	public String getMessage() {
		return message;
	}
	
	// --
	
	/**
	 * 格式化通知消息 HTML，支持 Markdown 语法
	 * 
	 * @param message
	 * @return
	 * @see MarkdownUtils
	 */
	public static String formatHtml(String message) {
		// Matchs any `@ID`
		Pattern atPattern = Pattern.compile("(\\@[0-9a-z\\-]{20})");
		Matcher atMatcher = atPattern.matcher(message);
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
