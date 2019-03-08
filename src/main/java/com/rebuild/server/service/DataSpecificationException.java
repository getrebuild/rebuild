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

package com.rebuild.server.service;

import com.rebuild.server.RebuildException;

/**
 * 违反数据约束相关异常。如非空/重复值/无效值等
 * 
 * @author devezhao
 * @since 11/26/2018
 */
public class DataSpecificationException extends RebuildException {
	private static final long serialVersionUID = -1636949017780407060L;

	// 业务码
	private int errorCode = 400;
	
	public DataSpecificationException() {
		super();
	}

	public DataSpecificationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public DataSpecificationException(String msg) {
		super(msg);
	}

	public DataSpecificationException(Throwable cause) {
		super(cause);
	}
	
	public DataSpecificationException(int errorCode, String msg) {
		super(msg);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
