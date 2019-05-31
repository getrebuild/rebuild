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

import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/03/16
 */
public class FormsDefaultValueTest extends TestSupport {

	@Test
	public void testExprDefaultValue() throws Exception {
		Entity e = MetadataHelper.getEntity(TEST_ENTITY);
		Field date = e.getField("date");
		
		System.out.println(FormsDefaultValue.exprDefaultValue(date, "{NOW}"));
		System.out.println(FormsDefaultValue.exprDefaultValue(date, "{NOW - 1H}"));
		System.out.println(FormsDefaultValue.exprDefaultValue(date, "{NOW + 1M}"));
		System.out.println(FormsDefaultValue.exprDefaultValue(date, "2019-09-01"));
		System.out.println(FormsDefaultValue.exprDefaultValue(date, "2019-09-01 01:01"));
	}
	
	@Test
	public void testSetFormInitialValue() throws Exception {
		addExtTestEntities(false);
		
		Entity SalesOrder999 = MetadataHelper.getEntity("SalesOrder999");
		Entity SalesOrderItem999 = MetadataHelper.getEntity("SalesOrderItem999");
		
		JSONObject initial = JSONUtils.toJSONObject(FormsDefaultValue.DV_MASTER, ID.newId(SalesOrder999.getEntityCode()));
		
		JSONArray elements = new JSONArray();
		JSONObject el = new JSONObject();
		el.put("field", "SalesOrder999Id");
		elements.add(el);
		
		JSONObject mockModel = new JSONObject();
		mockModel.put("elements", elements);
		
		FormsDefaultValue.setFormInitialValue(SalesOrderItem999, mockModel, initial);
	}
}
