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
public class InvalidRequestException extends RebuildException {
	private static final long serialVersionUID = 1104144276994648297L;

	private int errCode = 400;
	
	public InvalidRequestException() {
		super();
	}

	public InvalidRequestException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public InvalidRequestException(String msg) {
		super(msg);
	}

	public InvalidRequestException(Throwable cause) {
		super(cause);
	}
	
	public InvalidRequestException(int errCode, String msg) {
		super(msg);
	}
	
	@Override
	protected int getErrCode() {
		return errCode;
	}
}
