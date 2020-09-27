package com.rebuild.core.support.distributed;

import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

public class DistributedSupportTest extends TestSupport {

    @Test
    public void testMap() {
        RedissonClient redissonClient = Redisson.create();

        RMap<String, User> users = redissonClient.getMap("RB_USER");
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