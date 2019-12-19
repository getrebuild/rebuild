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
import com.rebuild.server.service.bizz.privileges.User;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
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
		Application.getUserStore().getAllTeams();
		
		Application.getUserStore().getUser(UserService.ADMIN_USER);
		Application.getUserStore().getDepartment(DepartmentService.ROOT_DEPT);
		Application.getUserStore().getRole(RoleService.ADMIN_ROLE);
		Application.getUserStore().getTeam(SIMPLE_TEAM);
	}
	
	@Test
	public void testRefresh() throws Exception {
		Application.getUserStore().refreshUser(UserService.ADMIN_USER);
		Application.getUserStore().refreshDepartment(DepartmentService.ROOT_DEPT);
		Application.getUserStore().refreshRole(RoleService.ADMIN_ROLE);
		Application.getUserStore().refreshTeam(SIMPLE_TEAM);
	}
	
	@Test
	public void testExists() throws Exception {
		assertTrue(Application.getUserStore().existsUser("admin"));
        assertFalse(Application.getUserStore().existsUser("not_exists"));
	}

	@Test
	public void testMemberToTeam() {
		Application.getSessionStore().set(SIMPLE_USER);
		try {
			Application.getBean(TeamService.class).createMembers(SIMPLE_TEAM, Collections.singletonList(SIMPLE_USER));
			User user = Application.getUserStore().getUser(SIMPLE_USER);
			System.out.println(user.getOwningTeams());

            assertFalse(user.getOwningTeams().isEmpty());
			assertTrue(Application.getUserStore().getTeam(SIMPLE_TEAM).isMember(SIMPLE_USER));

			Application.getBean(TeamService.class).deleteMembers(SIMPLE_TEAM, Collections.singletonList(SIMPLE_USER));
			System.out.println(user.getOwningTeams());
		} finally {
			Application.getSessionStore().clean();
		}
	}

	@Test
	public void existsAny() {
	    Application.getUserStore().existsAny(RoleService.ADMIN_ROLE);
    }
}
