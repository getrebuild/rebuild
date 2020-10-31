/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author devezhao
 * @since 10/17/2020
 */
class BaseControllerTest extends TestSupport {

    @Test
    public void getParameters() {
        BaseController c = new BaseController() {
        };

        MockHttpServletRequest request = MockMvcRequestBuilders
                .get("/user/login?name=a&int=123456&id=" + SIMPLE_USER)
                .buildRequest(Objects.requireNonNull(((WebApplicationContext) Application.getContext()).getServletContext()));

        assertEquals(c.getParameter(request, "name"), "a");

        assertThrows(InvalidParameterException.class,
                () -> c.getParameterNotNull(request, "name2"));

        assertEquals(123456, c.getIntParameter(request, "int"));
        assertNull(c.getIntParameter(request, "int2"));

        assertEquals(SIMPLE_USER, c.getIdParameterNotNull(request, "id"));
        assertThrows(InvalidParameterException.class,
                () -> c.getParameterNotNull(request, "id2"));
        assertNull(c.getIdParameter(request, "id2"));
    }
}