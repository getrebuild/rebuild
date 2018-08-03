/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entitymanage;

import org.junit.Test;

import cn.devezhao.rebuild.server.service.bizuser.UserService;
import cn.devezhao.rebuild.server.service.entitymanage.Entity2Schema;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class Entity2SchemaTest {

	@Test
	public void testCreate() throws Exception {
		new Entity2Schema(UserService.ADMIN_USER).create("测试一把", null);
	}
}
