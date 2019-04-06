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
	
	protected static final String TEST_ENTITY = "TestAllFields";

	@BeforeClass
	public static void startup() throws Exception {
		LOG.warn("TESTING Startup ...");
		if ("true".equals(System.getenv("TRAVIS"))) {
			LOG.warn("TESTING in TravisCI ...");
		}
		
		Application.debug();
		addTestEntityIfNeed(false);
	}
	
	@AfterClass
	public static void shutdown() {
		Application.getSessionStore().clean();
		LOG.warn("TESTING Shutdown ...");
	}
	
	/**
	 * 初始化测试实体
	 * 
	 * @param deleteExists
	 * @see DisplayType
	 */
	private static void addTestEntityIfNeed(boolean deleteExists) {
		if (MetadataHelper.containsEntity(TEST_ENTITY)) {
			if (deleteExists) {
				LOG.warn("Dropping test entity : " + TEST_ENTITY);
				new Entity2Schema(UserService.ADMIN_USER).drop(MetadataHelper.getEntity(TEST_ENTITY), true);
			} else {
				return;
			}
		}
		
		LOG.warn("Adding test entity : " + TEST_ENTITY);
		
		Entity2Schema entity2Schema = new Entity2Schema(UserService.ADMIN_USER);
		String entityName = entity2Schema.create(TEST_ENTITY.toUpperCase(), null, null, true);
		Entity testEntity = MetadataHelper.getEntity(entityName);
		
		for (DisplayType dt : DisplayType.values()) {
			if (dt == DisplayType.ID || dt == DisplayType.LOCATION || dt == DisplayType.ANYREFERENCE
					|| dt == DisplayType.BOOL || dt == DisplayType.AVATAR) {
				continue;
			}
			
			String fieldName = dt.name().toUpperCase();
			if (dt == DisplayType.REFERENCE) {
				new Field2Schema(UserService.ADMIN_USER).create(testEntity, fieldName, dt, null, entityName, null);
			} else {
				new Field2Schema(UserService.ADMIN_USER).create(testEntity, fieldName, dt, null);
			}
		}
	}
}
