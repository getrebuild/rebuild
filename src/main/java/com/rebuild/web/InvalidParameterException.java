/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import com.rebuild.core.DefinedException;

/**
 * 无效请求参数
 *
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class InvalidParameterException extends DefinedException {
    private static final long serialVersionUID = 1104144276994648297L;

    public InvalidParameterException(String msg) {
        super(msg, null, false, false);
    }
}
