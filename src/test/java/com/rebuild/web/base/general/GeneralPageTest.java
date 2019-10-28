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

package com.rebuild.web.base.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 
 * @author devezhao
 * @since 01/14/2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class GeneralPageTest extends TestSupportWithMVC {

	@Test
	public void testListPage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/app/TestAllFields/list");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	@Test
	public void testViewPage() throws Exception {
		ID testUser = UserService.ADMIN_USER;
		Entity testEntity = MetadataHelper.getEntity(TEST_ENTITY);
		Record testRecord = EntityHelper.forNew(testEntity.getEntityCode(), testUser);
		
		Application.getSessionStore().set(testUser);
		testRecord = Application.getEntityService(testEntity.getEntityCode()).create(testRecord);
		Application.getSessionStore().clean();
		
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/app/TestAllFields/view/" + testRecord.getPrimary());
		System.out.println(perform(builder, testUser));
	}
}
