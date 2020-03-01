/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

import com.rebuild.server.TestSupport;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class SysConfigurationTest extends TestSupport {

	@Test
	public void testKnowConfig() throws Exception {
		for (ConfigurableItem item : ConfigurableItem.values()) {
			String v = SysConfiguration.get(item);
			System.out.println(item + " = " + v);
		}
	}

	@Test(expected = SecurityException.class)
	public void getFileOfData() {
	    System.out.println(SysConfiguration.getFileOfTemp(null));
	    System.out.println(SysConfiguration.getFileOfTemp("../123.jpg"));
    }
}
