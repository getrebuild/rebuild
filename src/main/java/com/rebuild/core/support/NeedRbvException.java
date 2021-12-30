/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.core.DefinedException;

/**
 * @author devezhao
 * @since 2021/12/30
 */
public class NeedRbvException extends DefinedException {
    private static final long serialVersionUID = -5539922471794653867L;

    public NeedRbvException(String msg) {
        super(msg);
    }
}
