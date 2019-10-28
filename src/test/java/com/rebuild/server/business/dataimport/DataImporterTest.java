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

package com.rebuild.server.business.dataimport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * @author devezhao
 * @since 01/10/2019
 */
public class DataImporterTest extends TestSupport {

	@Test
	public void testParseRule() throws Exception {
		JSONObject rule = JSON.parseObject("{ file:'dataimports-test.csv', entity:'TestAllFields', repeat_opt:3, fields_mapping:{TestAllFieldsName:5} }");
		ImportRule importsEnter = ImportRule.parse(rule);
		System.out.println("ImportsEnter 1 : " + importsEnter);
		
		rule = JSON.parseObject("{ file:'dataimports-test.xls', entity:'TestAllFields', repeat_opt:1, repeat_fields:['TestAllFieldsName'], fields_mapping:{TestAllFieldsName:5} }");
		importsEnter = ImportRule.parse(rule);
		System.out.println("ImportsEnter 2 : " + importsEnter);
		
		rule = JSON.parseObject("{ file:'dataimports-test.xlsx', entity:'TestAllFields', repeat_opt:1, repeat_fields:['TestAllFieldsName'], fields_mapping:{TestAllFieldsName:5} }");
		importsEnter = ImportRule.parse(rule);
		System.out.println("ImportsEnter 3 : " + importsEnter);
	}
	
	// No `repeat_fields`
	@Test(expected=IllegalArgumentException.class)
	public void testErrorRule() throws Exception {
		JSONObject rule = JSON.parseObject("{ file:'dataimports-test.csv', entity:'TestAllFieldsName', repeat_opt:3, fields_mapping:{ TestAllFieldsName:5 } }");
		rule.remove("entity");
		ImportRule.parse(rule);
	}
	
	@Test
	public void testImports() throws Exception {
		JSONObject rule = JSON.parseObject("{ file:'dataimports-test.xlsx', entity:'TestAllFields', repeat_opt:2, repeat_fields:['TestAllFieldsName'], owning_user:'001-0000000000000001', fields_mapping:{TestAllFieldsName:5} }");
		ImportRule importsEnter = ImportRule.parse(rule);
		
		DataImporter dataImports = new DataImporter(importsEnter, UserService.ADMIN_USER);
		dataImports.run();
	}
}
