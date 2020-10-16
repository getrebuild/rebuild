/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/06
 */
public class ClassificationManagerTest extends TestSupport {

    @Test
    public void testFindByName() {
        Entity test = MetadataHelper.getEntity(TestAllFields);
        Field classification = test.getField("classification");

        ID itemId = ClassificationManager.instance.findItemByName("南京", classification);
        if (itemId != null) {
            String fullName = ClassificationManager.instance.getFullName(itemId);
            System.out.println(itemId + " > " + fullName);
        }
        System.out.println(itemId);

        itemId = ClassificationManager.instance.findItemByName("江苏.南京", classification);
        System.out.println(itemId);

        itemId = ClassificationManager.instance.findItemByName("江苏.无效的", classification);
        System.out.println(itemId);
    }
}
