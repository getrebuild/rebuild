/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata;

import cn.devezhao.persist4j.Field;
import com.rebuild.server.TestSupport;
import org.junit.Test;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/16
 */
public class DefaultValueHelperTest extends TestSupport {

	@Test
	public void testExprDefaultValue() throws Exception {
        Field dateField = MetadataHelper.getField(TEST_ENTITY, "DATE1");
		System.out.println(DefaultValueHelper.exprDefaultValue(dateField, "{NOW}"));
		System.out.println(DefaultValueHelper.exprDefaultValue(dateField, "{NOW - 1H}"));
		System.out.println(DefaultValueHelper.exprDefaultValue(dateField, "{NOW + 1M}"));
		System.out.println(DefaultValueHelper.exprDefaultValue(dateField, "2019-09-01"));
		System.out.println(DefaultValueHelper.exprDefaultValue(dateField, "2019-09-01 01:01"));
	}
}
