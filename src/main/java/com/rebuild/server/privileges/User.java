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

package com.rebuild.server.privileges;

import org.apache.commons.lang.StringUtils;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;

/**
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
	
	public String getEmail() {
		return email;
	}
	
	public String getFullName() {
		return StringUtils.defaultIfBlank(fullName, this.getName().toUpperCase());
	}
	
	public String getAvatarUrl() {
		return avatarUrl;
	}
	
	public BusinessUnit getOwningDept() {
		return super.getOwningBizUnit();
	}
}
