/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.core.RebuildException;

/**
 * 启动失败
 *
 * @author devezhao
 * @since 2026/4/11
 */
public class RebuildBootException extends RebuildException {
    private static final long serialVersionUID = -4486529825548597756L;

    public RebuildBootException(String message) {
        super(message);
    }

    public RebuildBootException(Throwable cause) {
        super(cause);
    }

    public RebuildBootException(String message, Throwable cause) {
        super(message, cause);
    }
}
