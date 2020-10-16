/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.dashboard.DashboardManager;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/09
 */
public class DashboardManagerTest extends TestSupport {

    @Test
    public void testGetList() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        JSON dashs = DashboardManager.instance.getAvailable(UserService.ADMIN_USER);
        System.out.println(dashs.toJSONString());
    }
}
