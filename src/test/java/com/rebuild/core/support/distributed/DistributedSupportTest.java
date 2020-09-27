/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

/**
 * @author devezhao
 * @since 2020/9/27
 */
public class DistributedSupportTest extends TestSupport {

    @Test
    public void testMap() {
        DistributedSupport support = Application.getBean(DistributedSupport.class);

        ConcurrentMap<String, User> users = support.getMap("RB_USER");
        users.put("User1", Application.getUserStore().getUser(UserService.SYSTEM_USER));

        User fromCache = users.get("User1");
        Assert.assertEquals(fromCache, Application.getUserStore().getUser(UserService.SYSTEM_USER));
        Assert.assertTrue(users.containsKey("User1"));
        Assert.assertFalse(users.containsKey("User2"));
        Assert.assertEquals(1, users.size());

        users.remove("User1");
        Assert.assertTrue(users.isEmpty());
    }
}