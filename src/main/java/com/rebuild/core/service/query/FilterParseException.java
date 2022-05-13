/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/30
 */
public class FilterParseException extends RebuildException {
    private static final long serialVersionUID = -8014993130546304986L;

    public FilterParseException(String msg) {
        super(msg);
    }
}
