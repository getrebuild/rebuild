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
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/16
 */
public class FieldValueHelperTest extends TestSupport {

    @Test
    public void parseDateExpr() {
        System.out.println(FieldValueHelper.parseDateExpr("{NOW}", null));
        System.out.println(FieldValueHelper.parseDateExpr("{NOW - 1H}", null));
        System.out.println(FieldValueHelper.parseDateExpr("{NOW + 1M}", null));
        System.out.println(FieldValueHelper.parseDateExpr("2019-09-01", null));
        System.out.println(FieldValueHelper.parseDateExpr("2019-09-01 01:01", null));
    }

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
