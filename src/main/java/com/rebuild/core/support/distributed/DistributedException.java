/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 2026/4/11
 */
public class DistributedException extends RebuildException {
    private static final long serialVersionUID = -7610464160008701559L;

    public DistributedException(String message) {
        super(message);
    }

    public DistributedException(Throwable cause) {
        super(cause);
    }

    public DistributedException(String message, Throwable cause) {
        super(message, cause);
    }
}
