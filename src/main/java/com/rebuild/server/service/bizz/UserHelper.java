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

package com.rebuild.server.service.bizz;

import java.util.HashSet;
import java.util.Set;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.server.service.bizz.privileges.User;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.persist4j.engine.ID;

/**
 * 用户帮助类
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class UserHelper {

	/**
	 * [显示名, 头像]
	 * 
	 * @param userId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public static String[] getShows(ID userId) throws NoMemberFoundException {
		User u = Application.getUserStore().getUser(userId);
		return new String[] { u.getFullName(), u.getAvatarUrl(true) };
	}
	
	/**
	 * 是否管理员
	 * 
	 * @param userId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public static boolean isAdmin(ID userId) throws NoMemberFoundException {
		return Application.getUserStore().getUser(userId).isAdmin();
	}
	
	/**
	 * 是否超级管理员
	 * 
	 * @param user
	 * @return
	 */
	public static boolean isSuperAdmin(ID userId) {
		return UserService.ADMIN_USER.equals(userId);
	}
	
	/**
	 * 是否激活
	 * 
	 * @param bizzId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public static boolean isActive(ID bizzId) throws NoMemberFoundException {
		if (bizzId.getEntityCode() == EntityHelper.User) {
			return Application.getUserStore().getUser(bizzId).isActive();
		} else if (bizzId.getEntityCode() == EntityHelper.Department) {
			return !Application.getUserStore().getDepartment(bizzId).isDisabled();
		} else if (bizzId.getEntityCode() == EntityHelper.Role) {
			return !Application.getUserStore().getRole(bizzId).isDisabled();
		}
		throw new NoMemberFoundException("Illegal ID for bizz: " + bizzId);
	}
	
	/**
	 * 获取用户部门
	 * 
	 * @param userId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public static Department getDepartment(ID userId) throws NoMemberFoundException {
		User u = Application.getUserStore().getUser(userId);
		return u.getOwningDept();
	}
	
	/**
	 * 获取所有子部门ID（包括自己）
	 * 
	 * @param parent
	 * @return
	 */
	public static Set<ID> getAllChildrenId(Department parent) {
		Set<ID> children = new HashSet<>();
		children.add((ID) parent.getIdentity());
		for (BusinessUnit child : parent.getAllChildren()) {
			children.add((ID) child.getIdentity());
		}
		return children;
	}
	
	/**
	 * 获取名称
	 * 
	 * @param bizzId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public static String getName(ID bizzId) throws NoMemberFoundException {
		if (bizzId.getEntityCode() == EntityHelper.User) {
			return Application.getUserStore().getUser(bizzId).getFullName();
		} else if (bizzId.getEntityCode() == EntityHelper.Department) {
			return Application.getUserStore().getDepartment(bizzId).getName();
		} else if (bizzId.getEntityCode() == EntityHelper.Role) {
			return Application.getUserStore().getRole(bizzId).getName();
		}
		throw new NoMemberFoundException("Illegal ID for bizz: " + bizzId);
	}
}
