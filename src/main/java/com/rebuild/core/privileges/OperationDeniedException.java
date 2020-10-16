/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import com.rebuild.core.RebuildException;

/**
 * 禁止操作/非法操作
 *
 * @author zhaofang123@gmail.com
 * @since 09/15/2020
 */
public class OperationDeniedException extends RebuildException {

    public OperationDeniedException(String msg) {
        super(msg);
    }
}
