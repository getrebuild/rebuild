/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import org.junit.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/24
 */
public class NotificationServiceTest extends TestSupport {

    @Test
    public void testSend() {
        Message msg = MessageBuilder.createMessage(SIMPLE_USER, "发一条消息", null);
        Application.getNotifications().send(msg);
        System.out.println("Notification Sent");
    }

    @Test
    public void testGetUnread() {
        Application.getNotifications().getUnreadMessage(SIMPLE_USER);
    }
}
