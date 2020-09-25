/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.entity;

import cn.devezhao.persist4j.Entity;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/30
 */
public class EasyMetaTest extends TestSupport {

    @Test
    public void getLabel() {
        Entity user = MetadataHelper.getEntity("User");
        EasyMeta.getLabel(user, "roleId.name");
        System.out.println(EasyMeta.getEntityShow(user));
    }

    @Test
    public void testEntities() {
        for (Entity entity : MetadataHelper.getEntities()) {
            System.out.println(entity);
        }
    }
}
