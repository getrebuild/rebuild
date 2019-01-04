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

package com.rebuild.server.service.bizz.privileges;

import java.io.Serializable;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.security.EntityPrivileges;

/**
 * 简单的权限定义
 * 
 * @author devezhao
 * @since 10/11/2018
 * @see EntityPrivileges
 */
public class ZeroPrivileges implements Privileges {
	private static final long serialVersionUID = 7185091441777921842L;
	
	// ZERO 权限
	
	// 登录
	public static final String AllowLogin = "0-AllowLogin";
	// 批量修改
	public static final String AllowBatchUpdate = "0-AllowBatchUpdate";
	// 数据导入
	public static final String AllowDataImport = "0-AllowDataImport";
	// 数据导出
	public static final String AllowDataOutput = "0-AllowDataOutput";
	
	// ----

	private final String zreoKey;
	private final String definition;
	
	/**
	 * @param zreoKey
	 * @param definition
	 */
	public ZeroPrivileges(String zreoKey, String definition) {
		this.zreoKey = zreoKey;
		this.definition = definition;
	}
	
	@Override
	public Serializable getIdentity() {
		return zreoKey;
	}
	
	@Override
	public boolean allowed(Permission action) {
		return allowed(action, null);
	}

	@Override
	public boolean allowed(Permission action, Serializable targetGuard) {
		return definition.contains(":4");  // {"Z":4}
	}

	@Override
	public DepthEntry superlative(Permission action) {
		return BizzDepthEntry.GLOBAL;
	}
}
