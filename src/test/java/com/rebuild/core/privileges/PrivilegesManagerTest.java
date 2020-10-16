/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.ZeroEntry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class PrivilegesManagerTest extends TestSupport {

    @Test
    public void testEntityPrivileges() {
        int entity = MetadataHelper.getEntity(TestAllFields).getEntityCode();

        Application.getPrivilegesManager().allowCreate(SIMPLE_USER, entity);
        Application.getPrivilegesManager().allowDelete(SIMPLE_USER, entity);
        Application.getPrivilegesManager().allowUpdate(SIMPLE_USER, entity);
        Application.getPrivilegesManager().allowRead(SIMPLE_USER, entity);
        Application.getPrivilegesManager().allowAssign(SIMPLE_USER, entity);
        Application.getPrivilegesManager().allowShare(SIMPLE_USER, entity);
        // Or
        Application.getPrivilegesManager().allow(SIMPLE_USER, entity, BizzPermission.CREATE);
    }

    @Test
    public void testZeroPrivileges() {
        Application.getPrivilegesManager().allow(SIMPLE_USER, ZeroEntry.AllowLogin);
    }

    @Test
    public void testAllow() {
        Entity test = MetadataHelper.getEntity(Account);
        boolean allowAccount = Application.getPrivilegesManager().allow(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
        assertFalse(allowAccount);

        test = MetadataHelper.getEntity(SalesOrderItem);
        boolean allowSalesOrderItem = Application.getPrivilegesManager().allow(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
        assertFalse(allowSalesOrderItem);
    }
}
