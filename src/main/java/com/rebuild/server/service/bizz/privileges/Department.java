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

package com.rebuild.server.service.bizz.privileges;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.engine.ID;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 部门
 * 
 * @author devezhao
 * @since 10/08/2018
 */
public class Department extends BusinessUnit {
	private static final long serialVersionUID = -5308455934676294159L;

	public Department(Serializable identity, String name, boolean disabled) {
		super(identity, name, disabled);
	}
	
	/**
	 * 是否下级部门
	 * 
	 * @param child
	 * @return
	 */
	public boolean isChildren(ID child) {
		for (BusinessUnit dept : getChildren()) {
			if (dept.getIdentity().equals(child)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 是否子部门（所有）
	 * 
	 * @param child
	 * @return
	 */
	public boolean isChildrenAll(Department child) {
		for (BusinessUnit dept : getChildren()) {
			if (dept.getIdentity().equals(child)) {
				return true;
			} else {
				return ((Department) dept).isChildrenAll(child);
			}
		}
		return false;
	}
	
	/**
	 * 获取子部门（包括所有子级）
	 * 
	 * @return
	 */
	public Set<BusinessUnit> getAllChildren() {
		Set<BusinessUnit> children = new HashSet<>(getChildren());
		for (BusinessUnit child : getChildren()) {
			children.addAll(((Department) child).getAllChildren());
		}
		return Collections.unmodifiableSet(children);
	}
	
	protected void cleanMember() {
		for (Principal u : allMembers) {
			removeMember(u);
		}
	}
}