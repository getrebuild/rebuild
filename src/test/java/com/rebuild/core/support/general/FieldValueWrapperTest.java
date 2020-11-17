/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.service.NoRecordFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/22
 */
public class FieldValueWrapperTest extends TestSupport {

    @Test
    public void testGetLabel() {
        System.out.println(FieldValueHelper.getLabel(SIMPLE_USER));
    }

    @Test
    public void testGetLabelThrow() {
        Assertions.assertThrows(NoRecordFoundException.class,
                () -> FieldValueHelper.getLabel(ID.newId(1)));
    }
}
