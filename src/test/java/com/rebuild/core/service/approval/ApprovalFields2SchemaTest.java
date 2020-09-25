/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/04
 */
public class ApprovalFields2SchemaTest extends TestSupport {

    @Test
    public void testCreateFields() {
        Entity test = MetadataHelper.getEntity(TestAllFields);
        boolean created = new ApprovalFields2Schema(UserService.ADMIN_USER).createFields(test);
        System.out.println("Fields of approval is created : " + created);
    }
}
