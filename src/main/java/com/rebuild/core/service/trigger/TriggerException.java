/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
public class TriggerException extends RebuildException {
    private static final long serialVersionUID = -2818312360045720505L;

    public TriggerException(String msg) {
        super(msg);
    }

    public TriggerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
