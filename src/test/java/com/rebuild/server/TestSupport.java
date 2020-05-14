/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.rbstore.MetaschemaImporter;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.Entity2Schema;
import com.rebuild.server.metadata.entity.Field2Schema;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.net.URL;

/**
 * 测试基类
 * 
 * @author devezhao
 * @since 01/03/2019
 */
public class TestSupport {
	
	protected static final Log LOG = LogFactory.getLog(TestSupport.class);
	
	// 测试专用实体
	protected static final String TEST_ENTITY = "TestAllFields";

    protected static final String Account = "Account999";
    protected static final String SalesOrder = "SalesOrder999";
    protected static final String SalesOrderItem = "SalesOrderItem999";

	// 示例用户
	protected static final ID SIMPLE_USER = ID.valueOf("001-9000000000000001");
	// 示例部门
	protected static final ID SIMPLE_DEPT = ID.valueOf("002-9000000000000001");
	// 示例角色（无任何权限）
	protected static final ID SIMPLE_ROLE = ID.valueOf("003-9000000000000001");
	// 示例团队
	protected static final ID SIMPLE_TEAM = ID.valueOf("006-9000000000000001");
	
	@BeforeClass
	public static void startup() throws Exception {
		LOG.warn("TESTING Startup ...");
		if ("true".equals(System.getenv("TRAVIS"))) {
			LOG.info("TESTING in TravisCI ...");
		}
		
		Application.debug();
		addTestEntityIfNeed(false);
	}
	
	@AfterClass
	public static void shutdown() {
		Application.getSessionStore().clean();
		LOG.warn("TESTING Shutdown ...");
	}

	// 初始化测试实体
	// @see DisplayType
	private static void addTestEntityIfNeed(boolean dropExists) {
		if (MetadataHelper.containsEntity(TEST_ENTITY)) {
			if (dropExists) {
				LOG.warn("Dropping test entity : " + TEST_ENTITY);
				new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(TEST_ENTITY), true);
			} else {
				return;
			}
		}
		
		LOG.warn("Adding test entity : " + TEST_ENTITY);
		
		Entity2Schema entity2Schema = new Entity2Schema(UserService.ADMIN_USER);
		String entityName = entity2Schema.createEntity(TEST_ENTITY.toUpperCase(), null, null, true);
		Entity testEntity = MetadataHelper.getEntity(entityName);
		
		for (DisplayType dt : DisplayType.values()) {
			if (dt == DisplayType.ID || dt == DisplayType.LOCATION || dt == DisplayType.ANYREFERENCE) {
				continue;
			}
			
			String fieldName = dt.name().toUpperCase();
			if (dt == DisplayType.REFERENCE) {
				new Field2Schema(UserService.ADMIN_USER).createField(testEntity, fieldName, dt, null, entityName, null);
			} else if (dt == DisplayType.CLASSIFICATION) {
				JSON area = JSON.parseObject("{classification:'018-0000000000000001'}");
				new Field2Schema(UserService.ADMIN_USER).createField(testEntity, fieldName, dt, null, entityName, area);
			} else if (dt == DisplayType.STATE) {
				JSON area = JSON.parseObject("{stateClass:'com.rebuild.server.helper.state.HowtoState'}");
				new Field2Schema(UserService.ADMIN_USER).createField(testEntity, fieldName, dt, null, entityName, area);
			} else {
				new Field2Schema(UserService.ADMIN_USER).createField(testEntity, fieldName, dt, null, null, null);
			}
		}
	}

	/**
	 * 获取测试类
	 *
	 * @return
	 */
	protected Entity getTestEntity() {
		addTestEntityIfNeed(false);
		return MetadataHelper.getEntity(TEST_ENTITY);
	}
	
	/**
	 * @param dropExists
	 * @throws Exception
	 * @see MetaschemaImporter
	 * @see Entity2Schema
	 */
	protected void addExtTestEntities(boolean dropExists) throws Exception {
		if (dropExists) {
			if (MetadataHelper.containsEntity(SalesOrder)) {
				new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(SalesOrder), true);	
			}
			if (dropExists && MetadataHelper.containsEntity(Account)) {
				new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(Account), true);	
			}
		}
		
		if (!MetadataHelper.containsEntity(Account)) {
			URL url = TestSupport.class.getClassLoader().getResource("metaschema.Account.json");
			String content = FileUtils.readFileToString(new File(url.toURI()));

			MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(content));
			if (this instanceof TestSupportWithUser) {
				TaskExecutors.exec(importer);
			} else {
				TaskExecutors.run(importer.setUser(UserService.ADMIN_USER));
			}
		}
		
		if (!MetadataHelper.containsEntity(SalesOrder)) {
			URL url = TestSupport.class.getClassLoader().getResource("metaschema.SalesOrder.json");
			String content = FileUtils.readFileToString(new File(url.toURI()));

			MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(content));
			if (this instanceof TestSupportWithUser) {
				TaskExecutors.exec(importer);
			} else {
				TaskExecutors.run(importer.setUser(UserService.ADMIN_USER));
			}
		}
	}
}
