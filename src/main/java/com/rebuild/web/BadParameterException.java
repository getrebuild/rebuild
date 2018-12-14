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

package com.rebuild.web;

import com.rebuild.server.RebuildException;

/**
 * 无效请求。如参数错误等
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class BadParameterException extends RebuildException {
	private static final long serialVersionUID = 1104144276994648297L;
	
	private int bizCode = 400;

	public BadParameterException() {
		this("无效请求参数");
	}
	
	public BadParameterException(String msg) {
		super(msg);
	}
	
	public BadParameterException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public BadParameterException(String msg, int bizCode) {
		super(msg);
		this.bizCode = bizCode;
	}
	
	@Override
	protected int getBizCode() {
		return bizCode;
	}
}
