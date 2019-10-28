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

package com.rebuild.server.service.query;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupportWithUser;
import org.junit.Test;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class AdvFilterParserTest extends TestSupportWithUser {

	@Override
	protected ID getSessionUser() {
		return SIMPLE_USER;
	}

	@Test
	public void testBaseParse() throws Exception {
		JSONObject filterExp = new JSONObject();
		filterExp.put("entity", "User");
		JSONArray items = new JSONArray();
		filterExp.put("items", items);
		filterExp.put("equation", "(1 AND 2) or (1 OR 2)");
		
		// Filter items
		items.add(JSON.parseObject("{ op:'LK', field:'loginName', value:'admin' }"));
		items.add(JSON.parseObject("{ op:'EQ', field:'deptId.name', value:'总部' }"));  // Joins
		
		String where = new AdvFilterParser(filterExp).toSqlWhere();
		System.out.println(where);
	}

	@Test
	public void testBadJoinsParse() throws Exception {
		JSONObject filterExp = new JSONObject();
		filterExp.put("entity", "User");
		JSONArray items = new JSONArray();
		filterExp.put("items", items);
		
		// Filter item
		items.add(JSON.parseObject("{ op:'LK', field:'loginName.name', value:'总部' }"));
		
		String where = new AdvFilterParser(filterExp).toSqlWhere();
		System.out.println(where);  // null
	}

	@Test
	public void testDateAndDatetime() throws Exception {
		JSONObject filterExp = new JSONObject();
		filterExp.put("entity", TEST_ENTITY);
		JSONArray items = new JSONArray();
		filterExp.put("items", items);

		// Use `=`
		items.add(JSON.parseObject("{ op:'EQ', field:'date', value:'2019-09-09' }"));
		// Use `between`
		items.add(JSON.parseObject("{ op:'EQ', field:'datetime', value:'2019-09-09' }"));
		System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

		items.clear();
		// Use `=`
		items.add(JSON.parseObject("{ op:'TDA', field:'date' }"));
		// Use `between`
		items.add(JSON.parseObject("{ op:'TDA', field:'datetime' }"));
		System.out.println(new AdvFilterParser(filterExp).toSqlWhere());

		items.clear();
		// No padding
		items.add(JSON.parseObject("{ op:'GT', field:'date', value:'2019-09-09' }"));
		// Padding time
		items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09' }"));
		// No padding
		items.add(JSON.parseObject("{ op:'GT', field:'datetime', value:'2019-09-09 12:12:54' }"));
		System.out.println(new AdvFilterParser(filterExp).toSqlWhere());
	}
}
