/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
public class PrivilegesManagerTest extends TestSupport {

	@Test
	public void testEntityPrivileges() {
		int entity = MetadataHelper.getEntity(TEST_ENTITY).getEntityCode();

		Application.getPrivilegesManager().allowCreate(SIMPLE_USER, entity);
		Application.getPrivilegesManager().allowDelete(SIMPLE_USER, entity);
		Application.getPrivilegesManager().allowUpdate(SIMPLE_USER, entity);
		Application.getPrivilegesManager().allowRead(SIMPLE_USER, entity);
		Application.getPrivilegesManager().allowAssign(SIMPLE_USER, entity);
		Application.getPrivilegesManager().allowShare(SIMPLE_USER, entity);
		// Or
		Application.getPrivilegesManager().allow(SIMPLE_USER, entity, BizzPermission.CREATE);
	}
	
	@Test
	public void testZeroPrivileges() {
		Application.getPrivilegesManager().allow(SIMPLE_USER, ZeroEntry.AllowLogin);
	}
	
	@Test
	public void testAllow() throws Exception {
		addExtTestEntities(false);
		
		Entity test = MetadataHelper.getEntity("Account999");
		boolean allowAccount = Application.getPrivilegesManager().allow(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
		assertTrue(!allowAccount);
		
		test = MetadataHelper.getEntity("SalesOrderItem999");
		boolean allowSalesOrderItem = Application.getPrivilegesManager().allow(SIMPLE_USER, ID.newId(test.getEntityCode()), BizzPermission.READ);
		assertTrue(!allowSalesOrderItem);
	}
}
