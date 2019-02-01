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

package com.rebuild.server.service.base;

import org.junit.Assert;
import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class GeneralEntityServiceTest extends TestSupport {
	
	@Test
	public void testGetEntityService() throws Exception {
		EntityService ies = Application.getEntityService(EntityHelper.User);
		Assert.assertTrue(ies.getEntityCode() == EntityHelper.User);
	}

	@Test
	public void testCRUD() throws Exception {
		Record record = EntityHelper.forNew(EntityHelper.Role, UserService.ADMIN_USER);
		record.setString("name", "测试角色");
		record = Application.getEntityService(EntityHelper.Role).create(record);
		
		ID roleId = record.getPrimary();
		System.out.println(Application.getUserStore().getRole(roleId).getName());
		
		record = EntityHelper.forUpdate(roleId, UserService.ADMIN_USER);
		record.setString("name", "测试角色-2");
		record = Application.getEntityService(EntityHelper.Role).createOrUpdate(record);
		
		System.out.println(Application.getUserStore().getRole(roleId).getName());
		
		Application.getEntityService(EntityHelper.Role).delete(roleId);
	}
}
