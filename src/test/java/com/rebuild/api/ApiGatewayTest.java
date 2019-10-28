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

package com.rebuild.api;

import com.rebuild.api.sdk.OpenApiSDK;
import com.rebuild.web.MvcResponse;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2019/7/23
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ApiGatewayTest extends TestSupportWithMVC {

    @Test
    public void testSimple() throws Exception {
        String apiUrl = "/gw/api/system-time?";
        Map<String, Object> bizParams = new HashMap<>();

        apiUrl += new OpenApiSDK("230853250", "LA31SVGBqxUT5ncjgfDItPMP7yh9bJJ4eyCjGmG0")
                .signMD5(bizParams);
        System.out.println("Request API : " + apiUrl);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(apiUrl);
        MvcResponse resp = perform(builder, null);
        System.out.println(resp);
    }
}
