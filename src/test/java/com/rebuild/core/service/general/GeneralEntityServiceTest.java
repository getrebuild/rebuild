/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.ServiceSpec;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class GeneralEntityServiceTest extends TestSupport {

    @Test
    public void getServiceSpec() {
        ServiceSpec ies = Application.getService(EntityHelper.User);
        Assert.assertEquals(ies.getEntityCode(), EntityHelper.User);
    }

    @Test
    public void CRUD() {
        Application.getSessionStore().set(UserService.ADMIN_USER);

        // 新建
        Record record = EntityHelper.forNew(EntityHelper.Role, UserService.ADMIN_USER);
        record.setString("name", "测试角色");
        record = Application.getService(EntityHelper.Role).create(record);

        ID roleId = record.getPrimary();
        System.out.println(Application.getUserStore().getRole(roleId).getName());

        // 更新
        record = EntityHelper.forUpdate(roleId, UserService.ADMIN_USER);
        record.setString("name", "测试角色-2");
        Application.getService(EntityHelper.Role).createOrUpdate(record);

        System.out.println(Application.getUserStore().getRole(roleId).getName());

        // 删除
        Application.getService(EntityHelper.Role).delete(roleId);
    }

    @Test
    public void getRecordsOfCascaded() {
        Application.getSessionStore().set(SIMPLE_USER);
        Application.getGeneralEntityService().getCascadedRecords(
                SIMPLE_USER,
                new String[]{"Role", "Department"},
                BizzPermission.DELETE);
    }

    @Test
    public void checkRepeated() {
        Record record = EntityHelper.forNew(MetadataHelper.getEntity(TestAllFields).getEntityCode(), SIMPLE_USER);
        record.setString("TESTALLFIELDSName", "123");

        List<Record> repeated = Application.getGeneralEntityService().getCheckRepeated(record, 100);
        System.out.println(JSON.toJSONString(repeated));
    }
}
