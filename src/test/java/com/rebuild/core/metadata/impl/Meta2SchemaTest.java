/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Zixin (RB)
 * @since 08/03/2018
 */
class Meta2SchemaTest extends TestSupport {

    @BeforeEach
    public void setUpPerMethod() {
        UserContextHolder.setUser(UserService.ADMIN_USER);
    }

    @Test
    void testCreateEntity() {
        String newEntityName = new Entity2Schema(UserService.ADMIN_USER)
                .createEntity("测试实体", null, null, false);
        System.out.println("New Entity is created : " + newEntityName);

        Entity newEntity = MetadataHelper.getEntity(newEntityName);
        boolean drop = new Entity2Schema(UserService.ADMIN_USER).dropEntity(newEntity);
        System.out.println("New Entity is dropped : " + newEntityName + " > " + drop);
    }

    @Test
    void testCreateField() {
        String newEntityName = new Entity2Schema(UserService.ADMIN_USER)
                .createEntity("测试字段", null, null, false);
        Entity newEntity = MetadataHelper.getEntity(newEntityName);

        String newFiled = new Field2Schema(UserService.ADMIN_USER)
                .createField(newEntity, "数字", DisplayType.NUMBER, null, null, null);
        System.out.println("New Field is created : " + newFiled);

        newEntity = MetadataHelper.getEntity(newEntityName);

        boolean drop = new Field2Schema(UserService.ADMIN_USER).dropField(newEntity.getField(newFiled), true);
        System.out.println("New Field is dropped : " + newFiled + " > " + drop);

        drop = new Entity2Schema(UserService.ADMIN_USER).dropEntity(newEntity);
        System.out.println("New Entity (for Field) is dropped : " + newEntityName + " > " + drop);
    }
}
