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

package com.rebuild.server.configuration.portals;

import cn.devezhao.commons.web.WebUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/09
 */
public class NavManagerTest extends TestSupport {

	@Test
	public void testGetNav() throws Exception {
		JSON nav = NavManager.instance.getNavLayout(UserService.ADMIN_USER);
		if (nav != null) {
			System.out.println("testGetNav .......... \n" + nav.toJSONString());
		}
	}
	
	@Test
	public void testPortalNav() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.get("/rebuild")
				.sessionAttr(WebUtils.CURRENT_USER, UserService.ADMIN_USER);
		HttpServletRequest request = builder.buildRequest(new MockServletContext());
		
		JSON navForPortal = NavManager.instance.getNavForPortal(request);
		System.out.println("testPortalNav .......... \n" + navForPortal.toJSONString());
		
		if (!((JSONArray) navForPortal).isEmpty()) {
			JSONObject firstNav = (JSONObject) ((JSONArray) navForPortal).get(0);
			String navHtml = NavManager.instance.renderNavItem(firstNav, "home");
			System.out.println(navHtml);
		}
	}
}
