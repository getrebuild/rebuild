/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.fieldvalue;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/22
 */
public class FieldValueWrapperTest extends TestSupport {

	@Test
	public void testGetLabel() throws Exception {
		System.out.println(FieldValueWrapper.getLabel(SIMPLE_USER));
	}
	
	@Test(expected = NoRecordFoundException.class)
	public void testGetLabelThrow() throws Exception {
		System.out.println(FieldValueWrapper.getLabel(ID.newId(1)));
	}
}
