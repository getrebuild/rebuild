/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import com.rebuild.core.RebuildException;
import com.rebuild.core.support.i18n.Language;

/**
 * 非法操作/禁止操作
 *
 * @author Zixin (RB)
 * @since 09/15/2020
 */
public class OperationDeniedException extends RebuildException {
    private static final long serialVersionUID = 2670636377089379190L;

    public OperationDeniedException() {
        super(Language.L("无权操作"));
    }

    public OperationDeniedException(String msg) {
        super(msg);
    }
}
