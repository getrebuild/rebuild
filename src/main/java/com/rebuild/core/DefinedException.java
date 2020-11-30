/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.api.Controller;

/**
 * 已知的业务异常（非系统错误）
 *
 * @author devezhao
 * @since 2020/10/13
 */
public class DefinedException extends RebuildException {
    private static final long serialVersionUID = 2504578210264915700L;

    // 错误码
    private int errorCode = Controller.CODE_ERROR;

    public DefinedException() {
        super();
    }

    public DefinedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DefinedException(String msg) {
        super(msg);
    }

    public DefinedException(Throwable cause) {
        super(cause);
    }

    public DefinedException(int errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    public DefinedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DefinedException(int errorCode, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
