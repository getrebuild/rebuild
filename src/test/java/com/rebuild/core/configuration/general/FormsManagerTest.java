/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import com.rebuild.TestSupport;
import com.rebuild.core.configuration.ConfigBean;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
class FormsManagerTest extends TestSupport {

    @Test
    void testGet() {
        ConfigBean f = FormsManager.instance.getNewFormLayout("User");
        System.out.println(f.toJSON());
    }
}
