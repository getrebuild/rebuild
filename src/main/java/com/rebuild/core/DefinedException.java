/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.api.Controller;

/**
 * @author devezhao
 * @since 2020/10/13
 */
public class DefinedException extends RebuildException {

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

    public int getErrorCode() {
        return errorCode;
    }
}
