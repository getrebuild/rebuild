/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import com.rebuild.TestSupport;
import com.rebuild.core.privileges.DepartmentService;
import com.rebuild.core.privileges.UserService;
import org.junit.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/24
 */
public class MessageBuilderTest extends TestSupport {

    @Test
    public void formatMessage() {
        String raw = "@" + UserService.ADMIN_USER + " 你好，这是一条消息，用户ID会特殊解析。"
                + "这是一条实体记录 @" + DepartmentService.ROOT_DEPT;

        String msg = MessageBuilder.formatMessage(raw);
        System.out.println("> " + raw + " \n> " + msg);
    }
}
