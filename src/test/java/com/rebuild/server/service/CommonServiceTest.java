/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/06/23
 */
public class CommonServiceTest extends TestSupport {

    @Test
    public void createOrUpdateAndDelete() {
        Entity entity = MetadataHelper.getEntity(TEST_ENTITY);
        Record record = EntityHelper.forNew(entity.getEntityCode(), SIMPLE_USER);
        record.setString("TestAllFieldsName", "CommonServiceTest" + System.currentTimeMillis());
        record = Application.getCommonService().create(record, false);
        System.out.println("New record : " + record.getPrimary());

        record = EntityHelper.forUpdate(record.getPrimary(), SIMPLE_USER);
        record.setString("TEXT", "CommonServiceTest" + System.currentTimeMillis());
        Application.getCommonService().update(record, false);
        System.out.println("Update record : " + record.getPrimary());

        Application.getCommonService().createOrUpdateAndDelete(new Record[0], new ID[] { record.getPrimary() }, false);
        System.out.println("Delete record : " + record.getPrimary());
    }

    // No privileges access
    @Test(expected = PrivilegesException.class)
    public void useStrictMode() {
        Entity entity = MetadataHelper.getEntity(TEST_ENTITY);
        Record record = EntityHelper.forNew(entity.getEntityCode(), SIMPLE_USER);
        record.setString("TestAllFieldsName", "CommonServiceTest" + System.currentTimeMillis());
        Application.getCommonService().create(record, true);
    }
}