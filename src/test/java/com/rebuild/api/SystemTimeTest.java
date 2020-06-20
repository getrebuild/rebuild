/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

/**
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public class SystemTimeTest {

	@Test
	public void execute() throws Exception {
		JSON ret = new SystemTime().execute(null);
		System.out.println(ret);
	}
}
