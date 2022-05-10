/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import com.rebuild.TestSupport;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class FormsManagerTest extends TestSupport {

    @Test
    public void testGet() {
        ConfigBean entry = FormsManager.instance.getFormLayout("User", UserService.ADMIN_USER);
        System.out.println(entry.toJSON());
    }
}
