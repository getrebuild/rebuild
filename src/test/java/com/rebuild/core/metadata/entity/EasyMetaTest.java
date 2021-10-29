/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.entity;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.EasyPhone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/30
 */
public class EasyMetaTest extends TestSupport {

    @Test
    void getLabel() {
        Entity user = MetadataHelper.getEntity("User");
        EasyMetaFactory.getLabel(user, "roleId.name");
        System.out.println(EasyMetaFactory.toJSON(user));
    }

    @Test
    void testEntities() {
        for (Entity entity : MetadataHelper.getEntities()) {
            System.out.println(entity);
        }
    }

    @Test
    void testPattern() {
        // 固话
        Assertions.assertFalse(EasyPhone.isPhone("021-123456"));
        Assertions.assertTrue(EasyPhone.isPhone("021-1234567"));
        Assertions.assertTrue(EasyPhone.isPhone("021-12345678"));
        Assertions.assertFalse(EasyPhone.isPhone("021-123456789"));
        Assertions.assertTrue(EasyPhone.isPhone("021-12345678-1"));
        Assertions.assertTrue(EasyPhone.isPhone("021-12345678-12"));
        Assertions.assertTrue(EasyPhone.isPhone("021-12345678-12345"));  // -分机
        Assertions.assertTrue(EasyPhone.isPhone("(86)021-12345678-12345"));  // (国际区号)
        // 手机
        Assertions.assertTrue(EasyPhone.isPhone("13712345678"));
        Assertions.assertFalse(EasyPhone.isPhone("1171234567"));
        Assertions.assertFalse(EasyPhone.isPhone("11712345678"));
        Assertions.assertFalse(EasyPhone.isPhone("12712345678"));
        Assertions.assertFalse(EasyPhone.isPhone("14712345678"));
    }
}
