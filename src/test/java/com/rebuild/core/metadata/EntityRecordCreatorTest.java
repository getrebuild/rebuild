/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author zhaofang123@gmail.com
 * @since 07/24/2021
 */
class EntityRecordCreatorTest extends TestSupport {

    @Test
    void testCreate() {
        // EMAIL1: Not well
        JSONObject forNew = JSON.parseObject(
                "{ metadata:{ entity:'TestAllFields' }, TestAllFieldsName:'forNew', EMAIL1:'123' }");

        EntityRecordCreator recordCreator = new EntityRecordCreator(
                MetadataHelper.getEntity(TestAllFields), forNew, SIMPLE_USER);
        Assertions.assertThrows(DataSpecificationException.class, recordCreator::create);
    }

    @Test
    void testUpdate() {
        // modifiedBy: Not update
        // TestAllFieldsName: Not null
        JSONObject forUpdate = JSON.parseObject(
                String.format("{ metadata:{ entity:'TestAllFields', id:'%s' }, TestAllFieldsName:'', modifiedBy:'%s' }",
                        ID.newId(0), UserService.SYSTEM_USER));

        EntityRecordCreator recordCreator = new EntityRecordCreator(
                MetadataHelper.getEntity(TestAllFields), forUpdate, SIMPLE_USER);
        Assertions.assertThrows(DataSpecificationException.class, recordCreator::create);
    }
}