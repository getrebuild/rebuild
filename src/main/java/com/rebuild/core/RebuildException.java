/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

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
