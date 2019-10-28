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

package com.rebuild.web;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.TestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controll 测试基类
 *
 * @author devezhao
 * @since 01/14/2019
 */
@ContextConfiguration("classpath:application-web.xml")
@WebAppConfiguration
public abstract class TestSupportWithMVC extends TestSupport {

    public static final String UA_WIN10_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

    @Autowired
    private WebApplicationContext context;

    protected MockMvc springMVC;

    @Before
    public void setUp() {
        springMVC = MockMvcBuilders.webAppContextSetup(context)
                .alwaysDo(MockMvcResultHandlers.print())
                .build();
        LOG.warn("TESTING setup SpringMVC Mock ... " + springMVC);
    }

    /**
     * @param builder
     * @return
     * @throws Exception
     */
    protected MvcResponse perform(MockHttpServletRequestBuilder builder) throws Exception {
        return perform(builder, null);
    }

    /**
     * @param builder
     * @param user
     * @return
     * @throws Exception
     */
    protected MvcResponse perform(MockHttpServletRequestBuilder builder, ID user) throws Exception {
        return perform(builder, user, MockMvcResultMatchers.status().isOk());
    }

    /**
     * @param builder
     * @return
     * @throws Exception
     */
    protected MvcResponse performRedirection(MockHttpServletRequestBuilder builder) throws Exception {
        return performRedirection(builder, null);
    }

    /**
     * @param builder
     * @param user
     * @return
     * @throws Exception
     */
    protected MvcResponse performRedirection(MockHttpServletRequestBuilder builder, ID user) throws Exception {
        return perform(builder, null, MockMvcResultMatchers.status().is3xxRedirection());
    }

    /**
     * @param builder
     * @param user
     * @param expect
     * @return
     * @throws Exception
     */
    protected MvcResponse perform(MockHttpServletRequestBuilder builder, ID user, ResultMatcher expect) throws Exception {
        builder.contentType("text/plain; charset=utf-8")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.USER_AGENT, UA_WIN10_CHROME);
        if (user != null) {
            builder.sessionAttr(WebUtils.CURRENT_USER, user);
        }

        ResultActions resultActions = springMVC.perform(builder);
        if (expect == null) {
            resultActions.andExpect(MockMvcResultMatchers.status().isOk());
        } else {
            resultActions.andExpect(expect);
        }

        MvcResult result = resultActions.andReturn();

        String content = result.getResponse().getContentAsString();
        if (StringUtils.isNotBlank(content)) {
            return new MvcResponse(content);
        }

        ModelAndView view = result.getModelAndView();
        return new MvcResponse(view);
    }
}
