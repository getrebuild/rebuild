/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/09
 */
public class PickListManagerTest extends TestSupport {

    @Test
    public void testGetPickList() {
        Field picklist = MetadataHelper.getEntity(TestAllFields).getField("picklist");
        JSON list = PickListManager.instance.getPickList(picklist);
        System.out.println(list.toJSONString());
    }
}
