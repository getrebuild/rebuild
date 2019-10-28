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

package com.rebuild.server.business.rbstore;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassificationImporterTest extends TestSupportWithMVC {

	@Test
	public void test0ListPage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.get("/admin/classifications")
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	@Test
	public void test0EditorPage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.get("/admin/classification/" + getClassification())
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	@Test
	public void test1Import() throws Exception {
		ClassificationImporter importer = new ClassificationImporter(
				UserService.ADMIN_USER, getClassification(), "CHINA-ICNEA.json");
		importer.run();
		System.out.println("ClassificationImporter : " + importer.getTotal());
	}
	
	@Test
	public void test9Delete() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-delete?id=" + getClassification())
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	private static ID lastAdded = null;
	private static ID getClassification() {
		if (lastAdded == null) {
			Record record = EntityHelper.forNew(EntityHelper.Classification, UserService.ADMIN_USER);
			record.setString("name", "测试" + System.currentTimeMillis());
			record = Application.getCommonService().create(record);
			lastAdded = record.getPrimary();
		}
		System.out.println("Mock Classification : " + lastAdded);
		return lastAdded;
	}
}
