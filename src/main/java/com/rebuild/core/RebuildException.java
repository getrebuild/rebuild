/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

/**
 * RB Root Exception
 *
 * @author Zixin (RB)
 * @since 05/19/2018
 */
public class RebuildException extends RuntimeException {
    private static final long serialVersionUID = -889444005870894361L;

    public RebuildException() {
    }

    public RebuildException(String message) {
        super(message);
    }

    public RebuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public RebuildException(Throwable cause) {
        super(cause);
    }

    public RebuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
