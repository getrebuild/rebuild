/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.log;

import ch.qos.logback.core.PropertyDefinerBase;
import com.rebuild.core.support.RebuildConfiguration;

/**
 * logback dir : ${DataDirectory}/_log/
 *
 * @author devezhao
 * @since 2020/10/20
 */
public class LogbackHomePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        return RebuildConfiguration.getFileOfData("_log").getAbsolutePath();
    }
}
