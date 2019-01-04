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

package com.rebuild.server.business.charts;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class ChartsTest extends TestSupport {

	@Test
	public void testIndex() throws Exception {
		Application.getSessionStore().set(UserService.ADMIN_USER);
		
		JSONObject config = JSON.parseObject(
				"{'entity':'User','title':'指标卡','type':'INDEX','axis':{'dimension':[],'numerical':[{'field':'userId','sort':'','calc':'COUNT'}]}}");
		ChartData index = ChartDataFactory.create(config);
		System.out.println(index.build());
	}
}
