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

package com.rebuild.server.helper;

import org.apache.commons.lang.math.RandomUtils;

/**
 * TODO 验证码
 * 
 * @author devezhao
 * @since 11/05/2018
 */
public class VCode {

	/**
	 * @param key
	 * @return
	 */
	public static String random(String key) {
		String vcode = RandomUtils.nextInt(999999999) + "888888";
		vcode = vcode.substring(0, 6);
		return vcode;
	}
	
	/**
	 * @param key
	 * @param vcode
	 * @return
	 */
	public static boolean verfiy(String key, String vcode) {
		return true;
	}
}
