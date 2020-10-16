/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 2020/06/23
 */
public class CommonsServiceTest extends TestSupport {

    @Test
    public void createOrUpdateAndDelete() {
        Entity entity = MetadataHelper.getEntity(TestAllFields);

        Record record = EntityHelper.forNew(entity.getEntityCode(), SIMPLE_USER);
        record.setString("TestAllFieldsName", "CommonsServiceTest" + System.currentTimeMillis());
        record = Application.getCommonsService().create(record, false);
        System.out.println("New record : " + record.getPrimary());

        record = EntityHelper.forUpdate(record.getPrimary(), SIMPLE_USER);
        record.setString("TEXT", "CommonsServiceTest" + System.currentTimeMillis());
        Application.getCommonsService().update(record, false);
        System.out.println("Update record : " + record.getPrimary());

        Application.getCommonsService().createOrUpdateAndDelete(new Record[0], new ID[]{record.getPrimary()}, false);
        System.out.println("Delete record : " + record.getPrimary());
    }

    @Test
    public void useStrictMode() {
        Entity entity = MetadataHelper.getEntity(TestAllFields);
        Record record = EntityHelper.forNew(entity.getEntityCode(), SIMPLE_USER);
        record.setString("TestAllFieldsName", "CommonsServiceTest" + System.currentTimeMillis());

        // No privileges access
        Assertions.assertThrows(PrivilegesException.class,
                () -> Application.getCommonsService().create(record, true));
    }
}