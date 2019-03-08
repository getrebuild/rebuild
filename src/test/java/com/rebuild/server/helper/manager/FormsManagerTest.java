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

package com.rebuild.server.helper.manager;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.TestSupport;
import com.rebuild.server.service.bizz.UserService;

/**
 * 
 * @author devezhao
 * @since 01/04/2019
 */
public class FormsManagerTest extends TestSupport {

	@Test
	public void testModel() throws Exception {
		JSON newModel = FormsManager.getFormModel("User", UserService.ADMIN_USER);
		System.out.println(newModel);
		
		JSON editModel = FormsManager.getFormModel("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
		System.out.println(editModel);
	}
	
	@Test
	public void testViewModel() throws Exception {
		JSON viewModel = FormsManager.getViewModel("User", UserService.ADMIN_USER, UserService.SYSTEM_USER);
		System.out.println(viewModel);
	}
}
