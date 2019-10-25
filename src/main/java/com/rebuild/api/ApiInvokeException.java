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

package com.rebuild.api;

import com.rebuild.server.RebuildException;

/**
 * API 根异常
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class ApiInvokeException extends RebuildException {
	private static final long serialVersionUID = -5069862757215287578L;
	
	// 内部错误
    public static final int ERR_SERVER = 500;
    // 业务错误
    public static final int ERR_BIZ = Controll.CODE_ERROR;
    // 鉴权错误
    public static final int ERR_BADAUTH = 401;
    // 无效API
    public static final int ERR_BADAPI = 402;
    // 超出请求频率
    public static final int ERR_FREQUENCY = 403;
    // 参数错误
    public static final int ERR_BADPARAMS = 410;
    // 违反数据约束
    public static final int ERR_DATASPEC = 420;


    private int errorCode = ERR_BIZ;

    public ApiInvokeException(String errorMsg) {
        super(errorMsg);
    }

    public ApiInvokeException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
    }

    public ApiInvokeException(int errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public ApiInvokeException(int errorCode, String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return this.getMessage();
    }
}
