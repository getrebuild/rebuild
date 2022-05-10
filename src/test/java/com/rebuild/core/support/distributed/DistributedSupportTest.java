/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

/**
 * @author devezhao
 * @since 2020/9/27
 */
class DistributedSupportTest extends TestSupport {

    @Test
    void testMap() {
        DistributedSupport distributedSupport = (DistributedSupport) Application.getContext().getBean("DistributedSupport");

        ConcurrentMap<String, User> users = distributedSupport.getMap("RB_USER");
        users.put("User1", Application.getUserStore().getUser(UserService.SYSTEM_USER));

        User fromCache = users.get("User1");
        Assertions.assertEquals(fromCache, Application.getUserStore().getUser(UserService.SYSTEM_USER));
        Assertions.assertTrue(users.containsKey("User1"));
        Assertions.assertFalse(users.containsKey("User2"));
        Assertions.assertEquals(1, users.size());

        users.remove("User1");
        Assertions.assertTrue(users.isEmpty());
    }
}