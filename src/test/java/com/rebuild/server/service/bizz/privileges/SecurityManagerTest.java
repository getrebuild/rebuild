/*
rebuild - Building your system freely.
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

import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.impl.BizzPermission;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public class SecurityManagerTest extends TestSupport {

	@Test
	public void testEntityPrivileges() throws Exception {
		int entity = MetadataHelper.getEntity(TEST_ENTITY).getEntityCode();
		
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.CREATE);
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.DELETE);
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.UPDATE);
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.READ);
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.ASSIGN);
		Application.getSecurityManager().allowed(EXAMPLE_USER, entity, BizzPermission.SHARE);
	}
	
	@Test
	public void testZero() throws Exception {
		Application.getSecurityManager().allowed(EXAMPLE_USER, ZeroEntry.AllowLogin);
	}
}
