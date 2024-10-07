/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 2023/8/28
 */
public class ReportsException extends RebuildException {
    private static final long serialVersionUID = -4178242018962437528L;

    public ReportsException(String msg) {
        super(msg);
    }

    public ReportsException(Throwable cause) {
        super(cause);
    }

    public ReportsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
