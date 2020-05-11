/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.rbstore;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassificationImporterTest extends TestSupportWithMVC {

	@Test
	public void test0ListPage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.get("/admin/classifications")
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	@Test
	public void test0EditorPage() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.get("/admin/classification/" + getClassification())
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	@Test
	public void test1Import() throws Exception {
		ClassificationImporter importer = new ClassificationImporter(getClassification(), "CHINA-ICNEA.json");
		TaskExecutors.run(importer.setUser(UserService.SYSTEM_USER));
		System.out.println("ClassificationImporter : " + importer.getSucceeded());
	}
	
	@Test
	public void test9Delete() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-delete?id=" + getClassification())
				.sessionAttr(WebUtils.KEY_PREFIX + "-AdminVerified", "Mock");
		System.out.println(perform(builder, UserService.ADMIN_USER));
	}
	
	protected static ID lastAdded = null;
    /**
     * 新建一个分类
     *
     * @return
     */
    protected static ID getClassification() {
		if (lastAdded == null) {
			Record record = EntityHelper.forNew(EntityHelper.Classification, UserService.ADMIN_USER);
			record.setString("name", "测试" + System.currentTimeMillis());
			record = Application.getCommonService().create(record);
			lastAdded = record.getPrimary();
		}
		System.out.println("Mock Classification : " + lastAdded);
		return lastAdded;
	}
}
