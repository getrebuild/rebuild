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

import java.io.File;

import org.apache.commons.io.FileUtils;

/**
 * 系统配置
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class SystemProps {

	/**
	 * @param file
	 * @return
	 */
	public static File getFileOfTemp(String file) {
		return new File(FileUtils.getTempDirectory(), file);
	}
	
	/**
	 * 云存储地址
	 * @return
	 */
	public static String getStorageUrl() {
		return "http://rb-cdn.errorpage.cn/";
	}
	
	/**
	 * 云存储账号
	 * @return
	 */
	public static String[] getStorageAccount() {
		return new String[] {
				"YCSTYJijko0gEoj84qx5NZjbshg2VzU7GE1l9FDe",
				"u1keXSO5otlajgiOGyF0QRWhFIQfVDi1D5-yEXv4",
				"rb-cdn"
		};
	}
}
