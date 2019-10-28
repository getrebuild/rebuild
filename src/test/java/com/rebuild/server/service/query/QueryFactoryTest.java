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

package com.rebuild.server.service.query;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class QueryFactoryTest extends TestSupport {
	
	@Test
	public void testBaseQuery() throws Exception {
		String sql = "select loginName from User";
		Filter filter = Application.getSecurityManager().createQueryFilter(SIMPLE_USER);
		Object[][] array = Application.getQueryFactory().createQuery(sql, filter).array();
		Assert.assertTrue(array.length > 0);
	}
	
	@Test
	public void testQueryAllDT() throws Exception {
 		Entity allDT = MetadataHelper.getEntity(TEST_ENTITY);
 		StringBuilder sql = new StringBuilder("select ");
 		for (Field f : allDT.getFields()) {
 			sql.append(f.getName()).append(',');
 			if (f.getType() == FieldType.REFERENCE) {
 				sql.append("&" + f.getName()).append(',');
 			}
 		}
 		sql.deleteCharAt(sql.length() - 1);
 		sql.append(" from ").append(allDT.getName());
 		System.out.println("Query : " + sql);
		
		Filter filter = Application.getSecurityManager().createQueryFilter(SIMPLE_USER);
		Application.getQueryFactory().createQuery(sql.toString(), filter).array();
	}
	
	@Test(expected=AccessDeniedException.class)
	public void testNoUser() throws Exception {
		Application.getQueryFactory().createQuery("select loginName from User").array();
	}
}
