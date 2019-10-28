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

package com.rebuild.server.business.approval;

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/04
 */
public class ApprovalFields2SchemaTest extends TestSupport {

	@Test
	public void testCreateFields() throws Exception {
		Entity test = MetadataHelper.getEntity(TEST_ENTITY);
		boolean created = new ApprovalFields2Schema(UserService.ADMIN_USER).createFields(test);
		System.out.println("Fields of approval is created : " + created);
	}
}
