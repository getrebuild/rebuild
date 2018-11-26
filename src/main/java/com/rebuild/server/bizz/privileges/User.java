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

package com.rebuild.server.bizz.privileges;

import org.apache.commons.lang.StringUtils;

import com.rebuild.server.ServerListener;
import com.rebuild.server.bizz.RoleService;
import com.rebuild.server.bizz.UserService;
import com.rebuild.server.helper.SystemConfiguration;

import cn.devezhao.persist4j.engine.ID;

/**
 * 用户
 * 
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class User extends cn.devezhao.bizz.security.member.User {
	private static final long serialVersionUID = 15823574375847575L;
	
	private String email;
	private String fullName;
	private String avatarUrl;
	
	public User(ID userId, String loginName, String email, String fullName, String avatarUrl, boolean disabled) {
		super(userId, loginName, disabled);
		this.email = email;
		this.fullName = fullName;
		this.avatarUrl = avatarUrl;
	}
	
	public ID getId() {
		return (ID) getIdentity();
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getFullName() {
		return StringUtils.defaultIfBlank(fullName, this.getName().toUpperCase());
	}
	
	public String getAvatarUrl() {
		return avatarUrl;
	}
	
	public String getAvatarUrl(boolean fullUrl) {
		if (!fullUrl) {
			return getAvatarUrl();
		}
		if (avatarUrl == null) {
			return ServerListener.getContextPath() + "/assets/img/avatar.png";
		} else {
			return SystemConfiguration.getStorageUrl() + getAvatarUrl() + "?imageView2/2/w/100/interlace/1/q/100";
		}
	}
	
	public Department getOwningDept() {
		return (Department) super.getOwningBizUnit();
	}
	
	/**
	 * 是否激活
	 */
	public boolean isActive() {
		if (isDisabled()) {
			return false;
		}
		return getOwningRole() != null && getOwningDept() != null;
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
		if (getOwningRole() != null && getOwningRole().getIdentity().equals(RoleService.ADMIN_ROLE)) {
			return true;
		}
		return false;
	}
}
