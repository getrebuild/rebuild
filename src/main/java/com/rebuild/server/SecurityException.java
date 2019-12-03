/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

/**
 * 业务安全异常
 *
 * @author ZHAO
 * @since 2019/12/3
 */
public class SecurityException extends RebuildException {
    private static final long serialVersionUID = 8786241299394987035L;

    public SecurityException() {
        super();
    }

    public SecurityException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SecurityException(String msg) {
        super(msg);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}
