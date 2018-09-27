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

package com.rebuild.server.service.bizuser;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseService;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class UserService extends BaseService {
	
	// 系统用户
	public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
	// 管理员
	public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");
	
	protected UserService(PersistManagerFactory persistManagerFactory) {
		super(persistManagerFactory);
	}

	@Override
	public int getEntity() {
		return EntityHelper.User;
	}
	
	public ID getDeptOfUser(ID user) {
		Object[] found = Application.createNoFilterQuery(
				"select deptId from User where userId = ?")
				.setParameter(1, user)
				.unique();
		return (ID) found[0];
	}
	
	// --
	
}
