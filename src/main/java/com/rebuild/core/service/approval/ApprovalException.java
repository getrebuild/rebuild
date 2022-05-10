/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class ApprovalException extends RebuildException {
    private static final long serialVersionUID = 7876166915760948592L;

    public ApprovalException(String msg) {
        super(msg);
    }
}
