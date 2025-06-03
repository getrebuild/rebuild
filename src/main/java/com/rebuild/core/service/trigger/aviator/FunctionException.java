/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.rebuild.core.RebuildException;

/**
 * @author Zixin (RB)
 * @since 6/3/2025
 */
public class FunctionException extends RebuildException {
    private static final long serialVersionUID = -1558990230035608546L;

    public FunctionException(String message) {
        super(message);
    }
}
