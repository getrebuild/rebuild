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

package cn.devezhao.rebuild.server.privileges;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.bizz.security.member.User;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.service.bizuser.RoleService;
import cn.devezhao.rebuild.server.service.bizuser.UserService;

/**
 * 实体安全/权限 管理
 * 
 * @author Zhao Fangfang
 * @version $Id: SecurityManager.java 25 2013-06-26 16:21:03Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-21
 */
public class SecurityManager {
	
	final private UserStore USER_STORE;

	public SecurityManager(UserStore us) {
		super();
		USER_STORE = us;
	}
	
	/**
	 * 获取记录所属人
	 * 
	 * @param recordId
	 * @return
	 */
	public ID getOwnUser(ID recordId) {
		return null;
	}
	
	/**
	 * 获取对某实体的权限
	 * 
	 * @param userId
	 * @param entity
	 * @return
	 */
	public Privileges getPrivileges(ID userId, int entity) {
		Role role = USER_STORE.getUser(userId).getOwningRole();
		return role.getPrivileges(entity);
	}
	
	/**
	 * 是否对某实体持有访问（读取）权限
	 * 
	 * @param userId
	 * @param entity
	 * @return
	 */
	public boolean allowedAccess(ID userId, int entity) {
		return allowed(userId, entity, BizzPermission.READ);
	}
	
	/**
	 * 是否对某实体的数据（数据所属人）持有访问（读取）权限
	 * 
	 * @param user
	 * @param entity
	 * @param targetRecord
	 * @return
	 */
	public boolean allowedAccess(ID user, int entity, ID targetRecord) {
		return allowed(user, entity, BizzPermission.READ, targetRecord);
	}
	
	/**
	 * 是否对某实体持有指定权限
	 * 
	 * @param user
	 * @param entity
	 * @param action
	 * @return
	 */
	public boolean allowed(ID user, int entity, Permission action) {
		if (UserService.ADMIN_USER.equals(user)) {
			return true;
		}
		
		Role role = USER_STORE.getUser(user).getOwningRole();
		if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
			return true;
		}
		
		Privileges priv = role.getPrivileges(entity);
		return priv.allowed(action);
	}
	
	/**
	 * 是否对某实体的数据（数据所属人）持有指定权限
	 * 
	 * @param user
	 * @param entity
	 * @param action
	 * @param targetRecord
	 * @return
	 */
	public boolean allowed(ID user, int entity, Permission action, ID targetRecord) {
		if (UserService.ADMIN_USER.equals(user)) {
			return true;
		}
		
		Role role = USER_STORE.getUser(user).getOwningRole();
		if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
			return true;
		}
		
		Privileges priv = role.getPrivileges(entity);
		boolean allowed = priv.allowed(action);
		if (!allowed) {
			return false;
		}
		
		DepthEntry de = priv.superlative(action);
		if (BizzDepthEntry.NONE.equals(de)) {
			return false;
		}
		if (user.equals(targetRecord)) {
			return true;
		}
		if (BizzDepthEntry.PRIVATE.equals(de)) {
			return false;
		}
		if (BizzDepthEntry.GLOBAL.equals(de)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 创建查询过滤器
	 * 
	 * @param user
	 * @return
	 */
	public QueryFilter createQueryFilter(ID user) {
		if (UserService.ADMIN_USER.equals(user)) {
			return QueryFilter.ALLOWED;
		}
		
		User sUser = USER_STORE.getUser(user);
		Role role = sUser.getOwningRole();
		if (RoleService.ADMIN_ROLE.equals(role.getIdentity())) {
			return QueryFilter.ALLOWED;
		}
		return new QueryFilter(sUser);
	}
}
