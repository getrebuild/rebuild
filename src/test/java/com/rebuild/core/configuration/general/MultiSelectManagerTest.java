/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class MultiSelectManagerTest extends TestSupport {

    @Test
    public void getSelectList() {
        Entity test = MetadataHelper.getEntity(TestAllFields);
        JSON ret = MultiSelectManager.instance.getSelectList(test.getField(DisplayType.MULTISELECT.name()));
        System.out.println(ret);
    }

    @Test
    public void getDefaultValue() {
        Entity test = MetadataHelper.getEntity(TestAllFields);
        Long ret = MultiSelectManager.instance.getDefaultValue(test.getField(DisplayType.MULTISELECT.name()));
        System.out.println(ret);
    }
}