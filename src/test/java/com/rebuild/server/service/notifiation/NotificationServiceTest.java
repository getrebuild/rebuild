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

package com.rebuild.server.service.notifiation;

import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.notification.Message;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/24
 */
public class NotificationServiceTest extends TestSupport {

	@Test
	public void testSend() throws Exception {
		Message minMessage = new Message(UserService.ADMIN_USER, "发一条消息", null);
		Application.getNotifications().send(minMessage);
		System.out.println("Notification Sent");
	}
}
