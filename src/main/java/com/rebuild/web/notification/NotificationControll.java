/*
rebuild - Building your system freely.
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

package com.rebuild.web.notification;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
public class NotificationControll extends BaseControll {

	@RequestMapping("/app/notifications")
	public ModelAndView pageIndex(HttpServletRequest request) throws IOException {
		return createModelAndView("/notification/index.jsp");
	}
	
	@RequestMapping("/app/notification/{id}")
	public ModelAndView pageDetails(@PathVariable String id, HttpServletRequest request) throws IOException {
		return createModelAndView("/notification/details.jsp");
	}
	
	@RequestMapping("/app/notification/check-message")
	public void checkMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Object[] unread = Application.createQueryNoFilter(
				"select count(messageId) from Notification where toUser = ? and unread = 'T'")
				.setParameter(1, user)
				.unique();
		
		Object[][] last5Msg = Application.createQueryNoFilter(
				"select fromUser,message,unread,messageId,createdOn from Notification where toUser = ? and unread = 'T' order by createdOn desc")
				.setParameter(1, user)
				.setLimit(5)
				.array();
		for (int i = 0; i < last5Msg.length; i++) {
			last5Msg[i] = formatMessage(last5Msg[i]);
		}
		
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "unread", "last" }, 
				new Object[] { unread[0], last5Msg });
		writeSuccess(response, ret);
	}
	
	@RequestMapping("/app/notification/list")
	public void list(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		int pn = getIntParameter(request, "page", 1);
		int ps = 20;
		
		Object[][] array = Application.createQueryNoFilter(
				"select fromUser,message,unread,messageId,createdOn from Notification where toUser = ? and unread = 'T' order by createdOn desc")
				.setParameter(1, user)
				.setLimit(ps, pn * ps - ps)
				.array();
		for (int i = 0; i < array.length; i++) {
			array[i] = formatMessage(array[i]);
		}
		
		writeSuccess(response, array);
	}
	
	/**
	 * @param message
	 * @return
	 */
	protected static Object[] formatMessage(Object[] message) {
		ID from = (ID) message[0];
		String fromShow[] = UserHelper.getShow(from);
		message[0] = fromShow;
		message[3] = message[3].toString();
		message[4] = CalendarUtils.getUTCDateTimeFormat().format(message[4]);
		
		String text = (String) message[1];
		text = text.replace("@" + from, "<a>" + fromShow[0] + "</a>");
		message[1] = text;
		return message;
	}
	
	@RequestMapping("/app/notification/toggle-unread")
	public void toggleUnread(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID messageId = getIdParameter(request, "id");
		String state = getParameter(request, "state");
		
		Record record = EntityHelper.forUpdate(messageId, user);
		record.setBoolean("unread", "unread".equalsIgnoreCase(state));
		Application.getCommonService().update(record);
		writeSuccess(response);
	}
}
