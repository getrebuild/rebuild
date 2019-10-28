/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.utils;

import com.rebuild.web.TestSupportWithMVC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/22
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class AppUtilsTest extends TestSupportWithMVC {
	
	@Test
	public void testGetErrorMessage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/dashboard/home");
		MvcResult result = springMVC.perform(builder).andReturn();
		System.out.println(AppUtils.getErrorMessage(result.getRequest(), null));
	}
	
	@Test
	public void testFormatControllMsg() throws Exception {
		System.out.println(AppUtils.formatControllMsg(600, "错误消息"));
		System.out.println(AppUtils.formatControllMsg(0, null));
	}
}
