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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/03
 */
public class FormsBuilderTest extends TestSupport {

	@Test
	public void testModel() throws Exception {
		JSON newModel = FormsBuilder.instance.buildForm("User", UserService.ADMIN_USER);
		System.out.println(newModel);
		
		JSON editModel = FormsBuilder.instance.buildForm("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
		System.out.println(editModel);
	}
	
	@Test
	public void testViewModel() throws Exception {
		JSON viewModel = FormsBuilder.instance.buildView("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
		System.out.println(viewModel);
	}


	@Test
	public void testSetFormInitialValue() throws Exception {
		addExtTestEntities(false);

		Entity SalesOrder999 = MetadataHelper.getEntity("SalesOrder999");
		Entity SalesOrderItem999 = MetadataHelper.getEntity("SalesOrderItem999");

		JSONObject initial = JSONUtils.toJSONObject(FormsBuilder.DV_MASTER, ID.newId(SalesOrder999.getEntityCode()));

		JSONArray elements = new JSONArray();
		JSONObject el = new JSONObject();
		el.put("field", "SalesOrder999Id");
		elements.add(el);

		JSONObject mockModel = new JSONObject();
		mockModel.put("elements", elements);

		FormsBuilder.instance.setFormInitialValue(SalesOrderItem999, mockModel, initial);
	}
}
