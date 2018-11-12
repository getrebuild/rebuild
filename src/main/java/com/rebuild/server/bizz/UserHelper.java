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

package com.rebuild.server.bizz;

import org.apache.commons.lang.StringUtils;

import com.rebuild.server.Application;
import com.rebuild.server.bizz.privileges.User;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class UserHelper {

	/**
	 * 显示名
	 * 
	 * @param user
	 * @return
	 */
	public static String getShowName(User user) {
		return StringUtils.defaultIfBlank(user.getFullName(), user.getName());
	}
	
	/**
	 * [显示名, 图像]
	 * 
	 * @param userId
	 * @return
	 */
	public static String[] getShows(ID userId) {
		User u = Application.getUserStore().getUser(userId);
		return new String[] { getShowName(u), u.getAvatarUrl(true) };
	}
	
	/**
	 * 是否管理员
	 * 
	 * @param user
	 * @return
	 */
	public static boolean isAdmin(ID userId) {
		return Application.getUserStore().getUser(userId).isAdmin();
	}
}
