/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.rebuild.core.RebuildException;

/**
 * API 根异常
 *
 * @author ZHAO
 * @since 2019-07-23
 */
public class ApiInvokeException extends RebuildException {
    private static final long serialVersionUID = -5069862757215287578L;

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

    private int errorCode = Controller.CODE_ERROR;

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
