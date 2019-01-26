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

package com.rebuild.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.Entity2Schema;
import com.rebuild.server.metadata.entityhub.Field2Schema;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Entity;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
public class TestSupport {
	
	protected static final Log LOG = LogFactory.getLog(TestSupport.class);
	
	protected static final String TEST_ENTITY = "TestAllFieldsRB10";

	@BeforeClass
	public static void startup() throws Exception {
		LOG.warn("TESTING Startup ...");
		if ("travis-ci".equals(System.getProperty("env"))) {
			TravisCI.install();
		}
		
		Application.debug();
		addTestEntityIfNeed();
	}
	
	@AfterClass
	public static void shutdown() {
		Application.getSessionStore().clean();
		LOG.warn("TESTING Shutdown ...");
	}
	
	/**
	 * 测试实体
	 * 
	 * @see DisplayType
	 */
	private static void addTestEntityIfNeed() {
		if (MetadataHelper.containsEntity(TEST_ENTITY)) {
			return;
		}
		
		LOG.warn("Adding test entity : " + TEST_ENTITY);
		
		Entity2Schema entity2Schema = new Entity2Schema(UserService.ADMIN_USER);
		String entityName = entity2Schema.create(TEST_ENTITY, null, null, true);
		Entity testEntity = MetadataHelper.getEntity(entityName);
		
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "number", DisplayType.NUMBER, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "decimal", DisplayType.DECIMAL, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "date", DisplayType.DATE, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "datetime", DisplayType.DATETIME, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "text", DisplayType.TEXT, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "ntext", DisplayType.NTEXT, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "email", DisplayType.EMAIL, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "url", DisplayType.URL, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "phone", DisplayType.PHONE, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "series", DisplayType.SERIES, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "image", DisplayType.IMAGE, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "file", DisplayType.FILE, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "picklist", DisplayType.PICKLIST, null, null);
		new Field2Schema(UserService.ADMIN_USER).create(testEntity, "reference", DisplayType.REFERENCE, null, entityName);
	}
}
