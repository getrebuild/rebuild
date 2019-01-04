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

package com.rebuild.server.metadata.entityhub;

import org.junit.Test;

import com.rebuild.server.TestSupport;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Entity;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class Meta2SchemaTest extends TestSupport {
	
	@Test
	public void testCreateEntity() throws Exception {
		String newEntityName = new Entity2Schema(UserService.ADMIN_USER).create("测试实体", null, null);
		System.out.println("New entity is created : " + newEntityName);
		
		Entity newEntity = MetadataHelper.getEntity(newEntityName);
		boolean drop = new Entity2Schema(UserService.ADMIN_USER).drop(newEntity);
		System.out.println("New entity is clear : " + newEntityName + " > " + drop);
	}
	
	@Test
	public void testCreateField() throws Exception {
		createField("数字", DisplayType.NUMBER, null);
		createField("货币", DisplayType.DECIMAL, null);
		
		boolean drop = new Entity2Schema(UserService.ADMIN_USER).drop(entity);
		System.out.println("Field entity is clear : " + entity + " > " + drop);
	}
	
	static Entity entity;
	static String createField(String fieldLabel, DisplayType type, String refEntity) {
		if (entity == null) {
			String newEntityName = new Entity2Schema(UserService.ADMIN_USER).create("测试字段实体", null, null);
			entity = MetadataHelper.getEntity(newEntityName);
		}
		String fieldName = new Field2Schema(UserService.ADMIN_USER).create(entity, fieldLabel, type, null, refEntity);
		System.out.println("New field is create : " + fieldName + " < " + fieldLabel);
		return fieldName;
	}
}
