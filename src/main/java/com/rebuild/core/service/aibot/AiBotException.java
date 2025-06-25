/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 2025/4/12
 */
public class AiBotException extends RebuildException {

    public AiBotException() {
        super();
    }

    public AiBotException(String message) {
        super(message);
    }

    public AiBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
