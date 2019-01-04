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

package com.rebuild.server.service.query;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class AdvFilterParserTest extends TestSupport {

	@Test
	public void testBaseParse() throws Exception {
		JSONObject filterExp = new JSONObject();
		filterExp.put("entity", "User");
		JSONArray items = new JSONArray();
		filterExp.put("items", items);
		
		// Filter entry
		items.add(JSON.parseObject("{ op:'LK', field:'loginName', value:'admin' }"));
		
		String where = new AdvFilterParser(filterExp).toSqlWhere();
		System.out.println(where);
	}
}
