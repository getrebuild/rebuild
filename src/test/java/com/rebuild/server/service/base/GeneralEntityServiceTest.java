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

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.ServiceSpec;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author devezhao
 * @since 01/04/2019
 */
public class GeneralEntityServiceTest extends TestSupportWithUser {

	@Override
	public ID getSessionUser() {
		return UserService.ADMIN_USER;
	}

	@Test
	public void getServiceSpec() throws Exception {
		ServiceSpec ies = Application.getService(EntityHelper.User);
		Assert.assertEquals(ies.getEntityCode(), EntityHelper.User);
	}

	@Test
	public void testCRUD() throws Exception {
		// 新建
		Record record = EntityHelper.forNew(EntityHelper.Role, UserService.ADMIN_USER);
		record.setString("name", "测试角色");
		record = Application.getService(EntityHelper.Role).create(record);
		
		ID roleId = record.getPrimary();
		System.out.println(Application.getUserStore().getRole(roleId).getName());

		// 更新
		record = EntityHelper.forUpdate(roleId, UserService.ADMIN_USER);
		record.setString("name", "测试角色-2");
		record = Application.getService(EntityHelper.Role).createOrUpdate(record);
		
		System.out.println(Application.getUserStore().getRole(roleId).getName());

		// 删除
		Application.getService(EntityHelper.Role).delete(roleId);
	}
	
	@Test
	public void getRecordsOfCascaded() throws Exception {
		Application.getSessionStore().set(SIMPLE_USER);
		Application.getGeneralEntityService().getRecordsOfCascaded(
				SIMPLE_USER,
				new String[] { "Role", "Department" },
				BizzPermission.DELETE);
	}

	@Test
	public void checkRepeated() {
		Record record = EntityHelper.forNew(TEST_ENTITY, SIMPLE_USER);
		record.setString("TESTALLFIELDSName", "123");

		List<Record> repeated = Application.getGeneralEntityService().checkRepeated(record);
		System.out.println(JSON.toJSONString(repeated));
	}

	@Test
	public void bulk() {

	}
}
