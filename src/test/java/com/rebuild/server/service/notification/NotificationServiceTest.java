/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.notification;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import org.junit.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/24
 */
public class NotificationServiceTest extends TestSupport {

	@Test
	public void testSend() throws Exception {
		Message minMessage = MessageBuilder.createMessage(SIMPLE_USER, "发一条消息");
		Application.getNotifications().send(minMessage);
		System.out.println("Notification Sent");
	}
	
	@Test
	public void testGetUnread() throws Exception {
		Application.getNotifications().getUnreadMessage(SIMPLE_USER);
	}
}
