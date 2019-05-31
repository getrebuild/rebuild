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

package com.rebuild.server.helper.datalist;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;

/**
 * @author zhaofang123@gmail.com
 * @since Jan 6, 2019
 */
public class DataListTest extends TestSupport {

	private JSONObject queryExpressie = null;
	
	@Before
	public void setup() {
		queryExpressie = JSON.parseObject("{ entity:'User' }");
		JSON fields = JSON.parseArray("[ 'userId', 'loginName', 'createdOn', 'createdBy' ]");
		queryExpressie.put("fields", fields);
		JSON filter = JSON.parseObject("{ entity:'User', type:'QUICK', values:{ 1:'admin' } }");
		queryExpressie.put("filter", filter);
		queryExpressie.put("sort", "createdOn:desc");
		queryExpressie.put("pageNo", 1);
		queryExpressie.put("pageSize", 100);
		System.out.println("Init query expressie ...");
	}
	
	@Test
	public void testQueryParser() throws Exception {
		JSONQueryParser queryParser = new JSONQueryParser(queryExpressie);
		System.out.println(queryParser.toSql());
		System.out.println(queryParser.toCountSql());
	}
	
	@Test
	public void testBase() throws Exception {
		DataList dlc = new DefaultDataList(queryExpressie, UserService.ADMIN_USER);
		System.out.println(dlc.getJSONResult());
	}
}
