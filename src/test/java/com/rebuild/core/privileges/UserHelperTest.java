/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.ServerStatus;
import com.rebuild.core.privileges.bizz.Department;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/30
 */
public class UserHelperTest extends TestSupport {

    @Test
    public void test() {
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
        for (int i = 0; i < 100; i++) {
            UserHelper.generateAvatar("你好", true);
            System.out.println(ServerStatus.getHeapMemoryUsed()[1]);
        }
    }

    @Test
    public void parseUsers() {
        JSONArray userDefs = new JSONArray();
        userDefs.add(SIMPLE_USER.toLiteral());
        userDefs.add(SIMPLE_TEAM.toLiteral());
        Set<ID> users = UserHelper.parseUsers(userDefs, null, true);

        System.out.println(Arrays.toString(users.toArray(new ID[0])));
    }
}
