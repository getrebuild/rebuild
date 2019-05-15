/*
rebuild - Building your system freely.
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
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.portals.value.FieldValueWrapper;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.MarkdownUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/23
 */
public class MessageHelper {

	/**
	 * 格式化通知消息
	 * 
	 * @param message
	 * @return
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
	
	/**
	 * @param atId
	 * @return
	 */
	private static String parseAtId(String atId) {
		if (!ID.isId(atId)) {
			return atId;
		}
		
		ID theId = ID.valueOf(atId);
		if (theId.getEntityCode() == EntityHelper.User) {
			if (Application.getUserStore().exists(theId)) {
				return Application.getUserStore().getUser(theId).getFullName();
			} else {
				return "[无效用户]";
			}
		}
		
		Entity entity = MetadataHelper.getEntity(theId.getEntityCode());
		String recordLabel = null;
		try {
			recordLabel = FieldValueWrapper.getLabel(theId);
		} catch (NoRecordFoundException ex) {
			recordLabel = "[无效记录]";
		}
		return MessageFormat.format("[{3}]({0}/app/{1}/list#!/View/{1}/{2})",
				AppUtils.getContextPath(), entity.getName(), theId, recordLabel);
	}
}
