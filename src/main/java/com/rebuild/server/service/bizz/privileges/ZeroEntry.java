/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

/**
 * 扩权限项
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public enum ZeroEntry {

	AllowLogin(true),

	AllowBatchUpdate(false),
	AllowDataImport(false),
	AllowDataOutput(false),

    AllowCustomNav(true),
    AllowCustomDataList(true),

	;
	
	private boolean defaultVal;
	
	ZeroEntry(boolean defaultVal) {
		this.defaultVal = defaultVal;
	}
	
	/**
	 * 默认值
	 * 
	 * @return
	 */
	public boolean getDefaultVal() {
		return defaultVal;
	}
}
