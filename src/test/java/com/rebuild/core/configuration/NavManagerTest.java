/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/09
 */
public class NavManagerTest extends TestSupport {

    @Test
    public void getNavLayout() {
        JSON nav = NavManager.instance.getNavLayout(UserService.ADMIN_USER);
        if (nav != null) {
            System.out.println("testGetNav .......... \n" + nav.toJSONString());
        }
    }

    @Test
    public void getNavPortal() {
        JSONArray navForPortal = NavBuilder.instance.getNavPortal(SIMPLE_USER);
        System.out.println("testPortalNav .......... \n" + navForPortal.toJSONString());

        if (!navForPortal.isEmpty()) {
            JSONObject firstNav = (JSONObject) navForPortal.get(0);
            String navHtml = NavBuilder.renderNavItem(firstNav, "home");
            System.out.println(navHtml);
        }
    }
}
