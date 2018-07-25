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

package cn.devezhao.rebuild.server.service.user;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.BaseService;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class UserService extends BaseService {
	
	/**
	 * 系统用户
	 */
	public static final ID SYS_USER = ID.valueOf("001-0000000000000000");
	/**
	 * 管理员
	 */
	public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");
	/**
	 * 根级部门
	 */
	public static final ID ROOT_DEPT = ID.valueOf("002-0000000000000001");
	/**
	 * 管理员权限
	 */
	public static final ID ADMIN_ROLE = ID.valueOf("003-0000000000000001");
	
	protected UserService(PersistManagerFactory persistManagerFactory) {
		super(persistManagerFactory);
	}

	@Override
	public int getEntity() {
		return EntityHelper.User;
	}
	
	public ID getDeptOfUser(ID user) {
		Object[] found = Application.createQuery(
				"select deptId from User where userId = ?")
				.setParameter(1, user)
				.unique();
		return (ID) found[0];
	}
}
