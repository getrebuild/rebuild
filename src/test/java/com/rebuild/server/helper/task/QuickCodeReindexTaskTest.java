/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.task;

import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.QuickCodeReindexTask;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class QuickCodeReindexTaskTest extends TestSupport {
	
	@Test
	public void testGenerateQuickCode() throws Exception {
		Assert.assertFalse("NHHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好     hello      世 界")));
		Assert.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("hello     world     ........")));
		Assert.assertTrue("HW".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("HelloWorld!")));
		Assert.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你好世界")));
		Assert.assertTrue("NHSJ".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("你 好           世 界")));
	}
	
	@Test
	public void testGenerateQuickCodeEmpty() throws Exception {
		// Phone, contains `-`
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("021-123-123")));
		// EMail, contains `@` and `.`
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("1234@getrebuild.com")));
		// URL
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("http://getrebuild.com/aswell")));
		
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("54325432543")));
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("helloworld")));
		Assert.assertTrue("".equalsIgnoreCase(QuickCodeReindexTask.generateQuickCode("123456helloworld")));
	}
	
	@Ignore
	@Test
	public void testReindex() throws Exception {
		new QuickCodeReindexTask(MetadataHelper.getEntity("User")).run();
		new QuickCodeReindexTask(MetadataHelper.getEntity("Role")).run();
		new QuickCodeReindexTask(MetadataHelper.getEntity("Department")).run();
	}
}
