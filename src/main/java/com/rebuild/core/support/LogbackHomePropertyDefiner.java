/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * logback dir : ${DataDirectory}/logs/
 *
 * @author devezhao
 * @since 2020/10/20
 */
public class LogbackHomePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        return RebuildConfiguration.getFileOfData("logs").getAbsolutePath();
    }
}
