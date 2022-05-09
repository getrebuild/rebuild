/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 12/20/2018
 */
public class ChartsException extends RebuildException {
    private static final long serialVersionUID = 1552569207578832059L;

    public ChartsException(String message) {
        super(message);
    }
}
