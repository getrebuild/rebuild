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

package com.rebuild.web.base.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.MvcResponse;
import com.rebuild.web.TestSupportWithMVC;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 
 * @author devezhao
 * @since 01/14/2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeneralOperatingControllTest extends TestSupportWithMVC {

	private static ID lastSaveId = ID.newId(999);  // It's fake

	@Test
	public void test1Save() throws Exception {
		JSONObject fromJson = JSON.parseObject("{ TestAllFieldsName:'Name" + System.currentTimeMillis() + "', metadata:{ entity:'TestAllFields' } }");
		
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-save")
				.content(fromJson.toJSONString());
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
		
		String recordId = resp.getDataJSONObject().getString("id");
		lastSaveId = ID.valueOf(recordId);
	}
	
	@Test
	public void test2Update() throws Exception {
		if (lastSaveId == null) {
			return;
		}
		JSONObject fromJson = JSON.parseObject("{ TestAllFieldsName:'test2', metadata:{ entity:'TestAllFields', id:'' } }");
		fromJson.getJSONObject("metadata").put("id", lastSaveId.toLiteral());
		
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-save")
				.content(fromJson.toJSONString());
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test3Assgin() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-assign?id=" + lastSaveId + "&to=" + UserService.SYSTEM_USER);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test4Share() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-share?id=" + lastSaveId + "&to=" + UserService.SYSTEM_USER);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test5UnshareBatch() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-unshare-batch?id=" + lastSaveId + "&to=" + UserService.SYSTEM_USER);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test6Unshare() throws Exception {
		Object[] accessId = Application.createQueryNoFilter(
				"select accessId from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
				.setParameter(1, "TestAllFields")
				.setParameter(2, lastSaveId)
				.setParameter(3, UserService.SYSTEM_USER)
				.unique();
		if (accessId == null) {
			return;
		}
		
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-unshare?record=" + lastSaveId + "&id=" + accessId[0]);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test7FetchRecordMeta() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-meta?id=" + lastSaveId);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
	
	@Test
	public void test9Delete() throws Exception {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
				.post("/app/entity/record-delete?id=" + lastSaveId);
		MvcResponse resp = perform(builder, UserService.ADMIN_USER);
		System.out.println(resp);
		Assert.assertTrue(resp.isSuccess());
	}
}
