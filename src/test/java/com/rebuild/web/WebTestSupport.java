/*
rebuild - Building your system freely.
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

package com.rebuild.web;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.rebuild.server.TestSupport;

/**
 * 
 * @author devezhao
 * @since 01/14/2019
 */
@ContextConfiguration("classpath:application-web.xml")
@WebAppConfiguration
public class WebTestSupport extends TestSupport {

	@Autowired
	private WebApplicationContext context;
	protected MockMvc theMVC;
	@Before
	public void setup() {
		theMVC = MockMvcBuilders.webAppContextSetup(context).build();
		LOG.warn("TESTING setup SpringMVC Mock ... " + theMVC);
	}
	
	/**
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	protected String performRedirection(MockHttpServletRequestBuilder builder) throws Exception {
		return perform(builder, true, true);
	}
	
	/**
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	protected String perform(MockHttpServletRequestBuilder builder) throws Exception {
		return perform(builder, true, false);
	}
	
	/**
	 * @param builder
	 * @param print
	 * @return
	 * @throws Exception
	 */
	protected String perform(MockHttpServletRequestBuilder builder, boolean print, boolean redirection) throws Exception {
		builder.contentType("text/plain;charset=utf-8")
				.accept(MediaType.APPLICATION_JSON_UTF8);
		
		ResultActions resultActions = theMVC.perform(builder);
		MvcResult result = resultActions
				.andExpect(redirection ? MockMvcResultMatchers.status().is3xxRedirection() : MockMvcResultMatchers.status().isOk())
				.andReturn();
		
		if (print) {
			resultActions.andDo(MockMvcResultHandlers.print());
		}
		return result.getResponse().getContentAsString();
	}
}
