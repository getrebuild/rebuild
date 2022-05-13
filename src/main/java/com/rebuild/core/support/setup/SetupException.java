/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 2019/11/27
 */
public class SetupException extends RebuildException {
    private static final long serialVersionUID = 2967864326290626538L;

    public SetupException(Throwable cause) {
        super(cause);
    }
}
