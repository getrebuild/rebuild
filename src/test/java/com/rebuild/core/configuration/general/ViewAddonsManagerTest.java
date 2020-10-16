/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 03/21/2020
 */
public class ViewAddonsManagerTest extends TestSupport {

    @Test
    public void getViewTab() {
        ViewAddonsManager.instance.getViewTab(TestAllFields, UserService.ADMIN_USER);
    }

    @Test
    public void getViewAdd() {
        ViewAddonsManager.instance.getViewAdd(TestAllFields, SIMPLE_USER);
    }
}