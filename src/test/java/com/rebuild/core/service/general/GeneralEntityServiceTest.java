/*!
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
import com.rebuild.core.RebuildException;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.ServiceSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author devezhao
 * @since 01/04/2019
 */
class GeneralEntityServiceTest extends TestSupport {

    @Test
    void getServiceSpec() {
        ServiceSpec ss = Application.getService(EntityHelper.User);
        Assertions.assertInstanceOf(UserService.class, ss);

        EntityService es = Application.getEntityService(MetadataHelper.getEntity(TestAllFields).getEntityCode());
        Assertions.assertInstanceOf(GeneralEntityService.class, es);

        boolean exThrows = false;
        try {
            Application.getEntityService(EntityHelper.User);
        } catch (RebuildException ok) {
            exThrows = true;
        }
        Assertions.assertTrue(exThrows);
    }

    @Test
    void CRUD() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        int testEntityCode = MetadataHelper.getEntity(TestAllFields).getEntityCode();

        // 新建
        Record record = EntityHelper.forNew(testEntityCode, UserService.ADMIN_USER);
        record.setString("TestAllFieldsName", "测试实体-1");
        record = Application.getEntityService(testEntityCode).create(record);

        Object create = Application.createQuery(
                "select TestAllFieldsName from TestAllFields where TestAllFieldsId = ?")
                .setParameter(1, record.getPrimary())
                .unique();
        Assertions.assertNotNull(create);

        // 更新
        record = EntityHelper.forUpdate(record.getPrimary(), UserService.ADMIN_USER);
        record.setString("TestAllFieldsName", "测试实体-1-1");
        Application.getEntityService(testEntityCode).createOrUpdate(record);

        // 删除
        Application.getEntityService(testEntityCode).delete(record.getPrimary());
    }

    @Test
    void getRecordsOfCascaded() {
        UserContextHolder.setUser(SIMPLE_USER);

        Application.getGeneralEntityService().getCascadedRecords(
                SIMPLE_USER,
                new String[]{"Role", "Department"},
                BizzPermission.DELETE);
    }

    @Test
    void checkRepeated() {
        Record record = EntityHelper.forNew(MetadataHelper.getEntity(TestAllFields).getEntityCode(), SIMPLE_USER);
        record.setString("TESTALLFIELDSName", "123");

        List<Record> repeated = Application.getGeneralEntityService().getAndCheckRepeated(record, 100);
        System.out.println(JSON.toJSONString(repeated));
    }

    @Test
    void processNVal() {
        ID newId = addRecordOfTestAllFields(UserService.SYSTEM_USER);

        Record record = EntityHelper.forNew(newId.getEntityCode());
        // 测试重复值
        record.setIDArray("N2NREFERENCE", new ID[] { newId, newId });
        record.setObjectValue("TAG1", new String[] { "AA", "AA" });
        Application.getGeneralEntityService().createOrUpdate(record);
    }
}
