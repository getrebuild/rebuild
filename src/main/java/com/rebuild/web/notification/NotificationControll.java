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

package com.rebuild.web.notification;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;

import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 系统通知
 *
 * @author devezhao
 * @since 11/01/2018
 *
 * @see com.rebuild.server.service.notification.Message
 */
@Controller
public class NotificationControll extends BasePageControll {

	@RequestMapping("/notifications")
	public ModelAndView pageIndex(HttpServletRequest request) throws IOException {
		return createModelAndView("/notification/messages.jsp");
	}

	@RequestMapping("/notifications/todo")
	public ModelAndView pageTodo(HttpServletRequest request) throws IOException {
		return createModelAndView("/notification/todo.jsp");
	}

	@RequestMapping("/notification/check-state")
	public void checkMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int unread = Application.getNotifications().getUnreadMessage(getRequestUser(request));
		writeSuccess(response, JSONUtils.toJSONObject("unread", unread));
	}

	@RequestMapping("/notification/make-read")
	public void toggleUnread(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String ids = getParameter(request, "id");

		if ("ALL".equalsIgnoreCase(ids)) {
			Object[][] unreads = Application.createQueryNoFilter(
					"select messageId from Notification where toUser = ?")
					.setParameter(1, user)
					.array();
			ids = "";
			for (Object[] o : unreads) {
				ids += o[0] + ",";
			}
		}

		for (String id : ids.split(",")) {
			if (!ID.isId(id)) {
				continue;
			}

			Record record = EntityHelper.forUpdate(ID.valueOf(id), user);
			record.setBoolean("unread", false);
			Application.getNotifications().update(record);
		}
		writeSuccess(response);
	}

	@RequestMapping("/notification/messages")
	public void listMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		int pn = getIntParameter(request, "page", 1);
		int ps = getIntParameter(request, "pageSize", 40);
		int type = getIntParameter(request, "type", 0);

		String sql = "select fromUser,message,createdOn,unread,messageId from Notification where toUser = ? and (1=1) order by createdOn desc";
		if (type == 1) {
			sql = sql.replace("(1=1)", "unread = 'T'");
		} else if (type == 2) {
			sql = sql.replace("(1=1)", "unread = 'F'");
		} else if (type == 10) {
			sql = sql.replace("(1=1)", "(type > 9 and type < 20)");
		} else if (type == 20) {
			sql = sql.replace("(1=1)", "(type > 19 and type < 30)");
		}

		Object[][] array = Application.createQueryNoFilter(sql)
				.setParameter(1, user)
				.setLimit(ps, pn * ps - ps)
				.array();
		
		for (int i = 0; i < array.length; i++) {
			Object[] m = array[i];
			m[0] = new Object[] { m[0], UserHelper.getName((ID) m[0]) };
			m[1] = MessageBuilder.toHTML((String) m[1]);
			m[2] = Moment.moment((Date) m[2]).fromNow();
			array[i] = m;
		}
		writeSuccess(response, array);
	}

	@RequestMapping("/notification/approvals")
	public void listApprovals(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		int pn = getIntParameter(request, "page", 1);
		int ps = getIntParameter(request, "pageSize", 40);
		int type = getIntParameter(request, "type", 0);

		String sql = "select fromUser,message,createdOn,unread,messageId from Notification where toUser = ? and (1=1) order by createdOn desc";
		if (type == 20) {
			sql = sql.replace("(1=1)", "unread = 'T' and type < 20");
		} else if (type == 2) {
			sql = sql.replace("(1=1)", "unread = 'F' and type < 20");
		} else if (type == 10) {
			sql = sql.replace("(1=1)", "(type > 9 and type < 20)");
		} else {
			sql = sql.replace("(1=1)", "type < 20");
		}

		Object[][] array = Application.createQueryNoFilter(sql)
				.setParameter(1, user)
				.setLimit(ps, pn * ps - ps)
				.array();

		for (int i = 0; i < array.length; i++) {
			Object[] m = array[i];
			m[0] = new Object[] { m[0], UserHelper.getName((ID) m[0]) };
			m[1] = MessageBuilder.toHTML((String) m[1]);
			m[2] = Moment.moment((Date) m[2]).fromNow();
			array[i] = m;
		}
		writeSuccess(response, array);
	}
}
