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

package com.rebuild.server.service.base;

import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/04
 */
public class BulkShareTest extends TestSupport {

	@Test
	public void testBulkShare() throws Exception {
		addExtTestEntities(false);
		Application.getSessionStore().set(UserService.ADMIN_USER);
		
		try {
			Entity entity = MetadataHelper.getEntity("Account999");
			Record record = EntityHelper.forNew(entity.getEntityCode(), UserService.ADMIN_USER);
			record.setString("accountName", "NAME" + System.currentTimeMillis());
			record = Application.getGeneralEntityService().create(record);
			
			BulkContext context = new BulkContext(
					UserService.ADMIN_USER, BizzPermission.SHARE, SIMPLE_USER, null, new ID[] { record.getPrimary() });
			Application.getGeneralEntityService().bulk(context);
		} finally {
			Application.getSessionStore().clean();
		}
	}
}
