/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.bizz.privileges;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.lang.StringUtils;

/**
 * 用户
 * 
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class User extends cn.devezhao.bizz.security.member.User {
	private static final long serialVersionUID = 15823574375847575L;
	
	private String email;
	private String workphone;
	private String fullName;
	private String avatarUrl;

	public User(ID userId, String loginName, String email, String workphone,
				String fullName, String avatarUrl, boolean disabled) {
		super(userId, loginName, disabled);
		this.email = email;
		this.workphone = workphone;
		this.fullName = fullName;
		this.avatarUrl = avatarUrl;
	}

	/**
	 * @return
	 */
	public ID getId() {
		return (ID) getIdentity();
	}

	/**
	 * @return
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @return
	 */
	public String getWorkphone() {
		return workphone;
	}

	/**
	 * @return
	 */
	public String getFullName() {
		return StringUtils.defaultIfBlank(fullName, this.getName().toUpperCase());
	}

	/**
	 * @return
	 */
	public String getAvatarUrl() {
		return avatarUrl;
	}

	/**
	 * @return
	 */
	public Department getOwningDept() {
		return (Department) super.getOwningBizUnit();
	}
	
	/**
	 * 是否管理员
	 * 
	 * @return
	 */
	public boolean isAdmin() {
		if (getIdentity().equals(UserService.ADMIN_USER)) {
			return true;
		}
		return getOwningRole() != null && getOwningRole().getIdentity().equals(RoleService.ADMIN_ROLE);
	}

	/**
	 * 是否激活/可用。如果用户所属部门或角色被禁用，用户同样也不可用
	 */
	@Override
	public boolean isActive() {
		if (isDisabled()) {
			return false;
		}
		if (getOwningDept() == null || getOwningDept().isDisabled()) {
			return false;
		}
		return getOwningRole() != null && !getOwningRole().isDisabled();
	}
}
