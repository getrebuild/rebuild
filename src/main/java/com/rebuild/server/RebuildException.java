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

package com.rebuild.server;

import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * RB Root Exception
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class RebuildException extends NestableRuntimeException {
	private static final long serialVersionUID = -889444005870894361L;

	public RebuildException() {
		super();
	}

	public RebuildException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public RebuildException(String msg) {
		super(msg);
	}

	public RebuildException(Throwable cause) {
		super(cause);
	}
}
