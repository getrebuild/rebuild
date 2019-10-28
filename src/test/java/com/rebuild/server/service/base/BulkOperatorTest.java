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

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/04
 */
public class BulkOperatorTest extends TestSupportWithUser {

	@Override
	public ID getSessionUser() {
		return UserService.ADMIN_USER;
	}

	@Test
	public void share() throws Exception {
		// 测试记录
		addExtTestEntities(false);
		ID recordNew = addRecordOfTestAllFields();

		// 共享
		BulkContext contextOfShare = new BulkContext(
				UserService.ADMIN_USER, BizzPermission.SHARE, SIMPLE_USER, null, new ID[] { recordNew });
		Application.getGeneralEntityService().bulk(contextOfShare);

        // 清理
        Application.getGeneralEntityService().delete(recordNew);
	}

	@Test
	public void assign() throws Exception {
		// 测试记录
		addExtTestEntities(false);
		ID recordNew = addRecordOfTestAllFields();

		// 共享
		BulkContext contextOfAssign = new BulkContext(
				UserService.ADMIN_USER, BizzPermission.ASSIGN, SIMPLE_USER, null, new ID[] { recordNew });
		Application.getGeneralEntityService().bulk(contextOfAssign);

        // 删除
        BulkContext contextOfDelete = new BulkContext(
                UserService.ADMIN_USER, BizzPermission.DELETE, null, null, new ID[] { recordNew });
        Application.getGeneralEntityService().bulk(contextOfDelete);
	}
}
