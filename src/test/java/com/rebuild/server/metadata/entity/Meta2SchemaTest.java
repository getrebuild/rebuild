/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
import org.junit.Test;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class Meta2SchemaTest extends TestSupportWithUser {

	@Test
	public void testCreateEntity() throws Exception {
		String newEntityName = new Entity2Schema(UserService.ADMIN_USER)
                .createEntity("测试实体", null, null, false);
		System.out.println("New Entity is created : " + newEntityName);
		
		Entity newEntity = MetadataHelper.getEntity(newEntityName);
		boolean drop = new Entity2Schema(UserService.ADMIN_USER).dropEntity(newEntity);
		System.out.println("New Entity is dropped : " + newEntityName + " > " + drop);

		addExtTestEntities(true);
	}
	
	@Test
	public void testCreateField() throws Exception {
		String newEntityName = new Entity2Schema(UserService.ADMIN_USER)
                .createEntity("测试字段", null, null, false);
		Entity newEntity = MetadataHelper.getEntity(newEntityName);
		
		String newFiled = new Field2Schema(UserService.ADMIN_USER)
                .createField(newEntity, "数字", DisplayType.NUMBER, null, null, null);
		System.out.println("New Field is created : " + newFiled);
		
		newEntity = MetadataHelper.getEntity(newEntityName);
		
		boolean drop = new Field2Schema(UserService.ADMIN_USER).dropField(newEntity.getField(newFiled), true);
		System.out.println("New Field is dropped : " + newFiled + " > " + drop);
		
		drop = new Entity2Schema(UserService.ADMIN_USER).dropEntity(newEntity);
		System.out.println("New Entity (for Field) is dropped : " + newEntityName + " > " + drop);
	}
}
