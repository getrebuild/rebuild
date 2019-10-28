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

package com.rebuild.server.service.bizz;

import com.rebuild.server.Application;
import com.rebuild.server.ServerStatus;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.privileges.Department;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/30
 */
public class UserHelperTest extends TestSupport {
	
	@Test
	public void test() throws Exception {
		UserHelper.getDepartment(UserService.ADMIN_USER);
		UserHelper.getName(DepartmentService.ROOT_DEPT);
		UserHelper.isActive(UserService.SYSTEM_USER);
		UserHelper.isAdmin(SIMPLE_USER);
		UserHelper.getMembers(RoleService.ADMIN_ROLE);
		UserHelper.getMembers(DepartmentService.ROOT_DEPT);
		
		Department dept = Application.getUserStore().getDepartment(DepartmentService.ROOT_DEPT);
		UserHelper.getAllChildren(dept);
		
		Application.getUserStore().getUser(SIMPLE_USER).isActive();
	}

    @Test
    public void generateAvatar() throws Exception {
		for (int i = 0; i < 10; i++) {
			UserHelper.generateAvatar("你好", true);
			System.out.println(ServerStatus.getHeapMemoryUsed()[1]);
		}
    }
}
