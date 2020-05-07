/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.bizz;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/2/6
 */
public class UserServiceTest extends TestSupportWithUser {

    @Override
    protected ID getSessionUser() {
        return UserService.SYSTEM_USER;
    }

    @Test
    public void users() {
        Record record = EntityHelper.forNew(EntityHelper.User, UserService.SYSTEM_USER);
        record.setString("loginName", "testuser" + RandomUtils.nextInt(999999));
        record.setString("password", CodecUtils.randomCode(10) + "Aa1!");
        record = Application.getBean(UserService.class).create(record);

        final ID userId = record.getPrimary();
        System.out.println("Created User : " + userId);

        Application.getBean(UserService.class)
                .updateEnableUser(userId, SIMPLE_DEPT, SIMPLE_ROLE, true);
        System.out.println("Enabled User : " + userId + " >> " + Application.getUserStore().getUser(userId).isActive());

        Application.getBean(UserService.class).delete(userId);
        System.out.println("Deleted User : " + userId);
    }
}