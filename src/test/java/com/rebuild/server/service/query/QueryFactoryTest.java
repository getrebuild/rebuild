/*
rebuild - Building your system freely.
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

package com.rebuild.server.service.query;

import org.junit.Assert;
import org.junit.Test;

import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Filter;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class QueryFactoryTest extends TestSupport {
	
	@Test
	public void testBaseQuery() throws Exception {
		Filter filter = Application.getSecurityManager().createQueryFilter(UserService.SYSTEM_USER);
		Object[][] array = Application.getQueryFactory().createQuery("select loginName from User", filter).array();
		Assert.assertTrue(array.length > 0);
	}
	
	@Test(expected=AccessDeniedException.class)
	public void testNoUser() throws Exception {
		Application.getQueryFactory().createQuery("select loginName from User").array();
	}
}
