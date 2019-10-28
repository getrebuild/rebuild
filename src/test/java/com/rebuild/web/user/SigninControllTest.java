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

package com.rebuild.web.user;

import com.rebuild.web.TestSupportWithMVC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author devezhao
 * @since 01/14/2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SigninControllTest extends TestSupportWithMVC {

	@Test
	public void testLogin() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/user/user-login?user=admin&passwd=111111");
		System.out.println(perform(builder, null));
	}
	
	@Test
	public void testPages() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/user/login");
		System.out.println(perform(builder));

		builder = MockMvcRequestBuilders.get("/user/signup");
		System.out.println(perform(builder, null, MockMvcResultMatchers.status().is4xxClientError()));
		
		builder = MockMvcRequestBuilders.get("/user/forgot-passwd");
		System.out.println(perform(builder));
		
		builder = MockMvcRequestBuilders.get("/user/logout");
		System.out.println(performRedirection(builder));
	}
}
