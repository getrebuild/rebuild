/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Test;

/**
 * @author Zixin (RB)
 * @since 12/17/2021
 */
class CopyEntityTest extends TestSupport {

    @Test
    void copy() {
        String sourceName = TestAllFields;

        String newName =
                ((CopyEntity) new CopyEntity(MetadataHelper.getEntity(sourceName)).setUser(UserService.ADMIN_USER))
                        .copy(sourceName + "Copy", null);
        System.out.println("New entity : " + newName);
    }
}