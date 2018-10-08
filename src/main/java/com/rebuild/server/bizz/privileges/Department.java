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

import java.io.Serializable;

import cn.devezhao.bizz.security.member.BusinessUnit;

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
}