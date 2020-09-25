/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import com.rebuild.api.Controller;
import com.rebuild.core.RebuildException;

/**
 * 违反数据约束相关异常。如非空/重复值/无效值等
 *
 * @author devezhao
 * @since 11/26/2018
 */
public class DataSpecificationException extends RebuildException {
    private static final long serialVersionUID = -1636949017780407060L;

    // 业务码
    private int errorCode = Controller.CODE_ERROR;

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
