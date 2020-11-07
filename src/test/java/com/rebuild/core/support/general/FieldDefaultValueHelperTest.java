/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import com.rebuild.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import org.junit.jupiter.api.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/16
 */
public class FieldDefaultValueHelperTest extends TestSupport {

    @Test
    public void testExprDefaultValue() {
        Field dateField = MetadataHelper.getField(TestAllFields, "DATE1");
        System.out.println(FieldDefaultValueHelper.exprDefaultValue(dateField, "{NOW}"));
        System.out.println(FieldDefaultValueHelper.exprDefaultValue(dateField, "{NOW - 1H}"));
        System.out.println(FieldDefaultValueHelper.exprDefaultValue(dateField, "{NOW + 1M}"));
        System.out.println(FieldDefaultValueHelper.exprDefaultValue(dateField, "2019-09-01"));
        System.out.println(FieldDefaultValueHelper.exprDefaultValue(dateField, "2019-09-01 01:01"));
    }
}
