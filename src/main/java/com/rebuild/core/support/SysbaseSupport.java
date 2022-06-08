/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author RB
 * @since 2022/6/8
 */
public class SysbaseSupport {

    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static File getLogbackFile() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        FileAppender fa = (FileAppender) lc.getLogger("ROOT").getAppender("FILE");
        return new File(fa.getFile());
    }
}
