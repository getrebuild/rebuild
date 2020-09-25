/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import com.rebuild.core.RebuildException;

/**
 * 修改元数据异常
 *
 * @author devezhao
 * @since 11/16/2018
 */
public class MetadataException extends RebuildException {
    private static final long serialVersionUID = 1552569207578832059L;

    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(Throwable cause) {
        super(cause);
    }
}
