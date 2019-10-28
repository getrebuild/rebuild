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

package com.rebuild.server.service.bizz.privileges;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class SecurityManagerTest extends TestSupport {

	@Test
	public void testEntityPrivileges() throws Exception {
		int entity = MetadataHelper.getEntity(TEST_ENTITY).getEntityCode();
		
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.CREATE);
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.DELETE);
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.UPDATE);
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.READ);
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.ASSIGN);
		Application.getSecurityManager().allowed(SIMPLE_USER, entity, BizzPermission.SHARE);
	}
	
	@Test
	public void testZero() throws Exception {
		Application.getSecurityManager().allowed(SIMPLE_USER, ZeroEntry.AllowLogin);
	}
	
	@Test
	public void testAllow() throws Exception {
		addExtTestEntities(false);
		
		Entity test = MetadataHelper.getEntity("Account999");
		boolean allowAccount = Application.getSecurityManager().allowed(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
		assertTrue(!allowAccount);
		
		test = MetadataHelper.getEntity("SalesOrderItem999");
		boolean allowSalesOrderItem = Application.getSecurityManager().allowed(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
		assertTrue(!allowSalesOrderItem);
	}
}
