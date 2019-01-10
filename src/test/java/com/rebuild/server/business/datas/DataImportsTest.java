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

package com.rebuild.server.business.datas;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;

/**
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public class DataImportsTest extends TestSupport {

	@Test
	public void testParseEnter() throws Exception {
		JSONObject rule = JSON.parseObject("{ file:'204648874__dataimports-test.csv', entity:'h45hy54hy', repeat_opt:3, fields_mapping:{ h45hy54hyName:5 } }");
		ImportsEnter importsEnter = ImportsEnter.parse(rule);
		System.out.println("ImportsEnter 1 : " + importsEnter);
		
		rule = JSON.parseObject("{ file:'204648874__dataimports-test.csv', entity:'h45hy54hy', repeat_opt:1, repeat_fields:['h45hy54hyName'], fields_mapping:{ h45hy54hyName:5 } }");
		importsEnter = ImportsEnter.parse(rule);
		System.out.println("ImportsEnter 2 : " + importsEnter);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testErrorEnter() throws Exception {
		JSONObject rule = JSON.parseObject("{ file:'204648874__dataimports-test.csv', entity:'h45hy54hy', repeat_opt:3, fields_mapping:{ h45hy54hyName:5 } }");
		rule.remove("entity");
		ImportsEnter.parse(rule);
	}
	
}
