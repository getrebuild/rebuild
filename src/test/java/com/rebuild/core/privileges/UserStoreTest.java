/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContext;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
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
    public void testStore() {
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
    public void testRefresh() {
        Application.getUserStore().refreshUser(UserService.ADMIN_USER);
        Application.getUserStore().refreshDepartment(DepartmentService.ROOT_DEPT);
        Application.getUserStore().refreshRole(RoleService.ADMIN_ROLE);
        Application.getUserStore().refreshTeam(SIMPLE_TEAM);
    }

    @Test
    public void testExists() {
        assertTrue(Application.getUserStore().existsUser("admin"));
        assertFalse(Application.getUserStore().existsUser("not_exists"));
    }

    @Test
    public void testMemberToTeam() {
        UserContext.setUser(UserService.SYSTEM_USER);

        ID teamUser = UserService.ADMIN_USER;
        try {
            Application.getBean(TeamService.class).createMembers(SIMPLE_TEAM, Collections.singletonList(teamUser));
            User user = Application.getUserStore().getUser(teamUser);
            System.out.println(user.getOwningTeams());

            assertFalse(user.getOwningTeams().isEmpty());
            assertTrue(Application.getUserStore().getTeam(SIMPLE_TEAM).isMember(teamUser));

            Application.getBean(TeamService.class).deleteMembers(SIMPLE_TEAM, Collections.singletonList(teamUser));
            System.out.println(user.getOwningTeams());

        } finally {
            UserContext.clear();
        }
    }

    @Test
    public void existsAny() {
        Application.getUserStore().existsAny(RoleService.ADMIN_ROLE);
    }

    @Test
    public void deptMethods() {
        Department dept = Application.getUserStore().getDepartment(SIMPLE_DEPT);
        System.out.println(dept.getAllChildren());
        System.out.println(dept.isChildren(DepartmentService.ROOT_DEPT));
    }
}
