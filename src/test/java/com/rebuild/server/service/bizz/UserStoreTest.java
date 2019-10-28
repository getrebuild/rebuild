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

package com.rebuild.server.service.bizz;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class UserStoreTest extends TestSupport {

	@Test
	public void testStore() throws Exception {
		Application.getUserStore().getAllUsers();
		Application.getUserStore().getAllDepartments();
		Application.getUserStore().getAllRoles();
		
		Application.getUserStore().getUser(UserService.ADMIN_USER);
		Application.getUserStore().getDepartment(DepartmentService.ROOT_DEPT);
		Application.getUserStore().getRole(RoleService.ADMIN_ROLE);
	}
	
	@Test
	public void testRefresh() throws Exception {
		Application.getUserStore().refreshUser(UserService.ADMIN_USER);
		Application.getUserStore().refreshDepartment(DepartmentService.ROOT_DEPT);
		Application.getUserStore().refreshRole(RoleService.ADMIN_ROLE, true);
	}
	
	@Test
	public void testExists() throws Exception {
		assertTrue(Application.getUserStore().exists("admin"));
		assertTrue(!Application.getUserStore().exists("not_exists"));
	}
}
