/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.notification;

import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.DepartmentService;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/24
 */
public class MessageBuilderTest extends TestSupport {

	@Test
	public void formatMessage() throws Exception {
		String raw = "@" + UserService.ADMIN_USER + " 你好，这是一条消息，用户ID会特殊解析。"
				+ "这是一条实体记录 @" + DepartmentService.ROOT_DEPT
				+ " 可以多条，不信你看  @" + RoleService.ADMIN_ROLE;
		String ff = MessageBuilder.formatMessage(raw);
		System.out.println("> " + raw + " \n> " + ff);
	}
}
