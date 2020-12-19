/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.rebuild.core.UserContextHolder;

/**
 * @author devezhao
 * @since 2020/12/19
 */
public class LogbackReqidConverter extends MessageConverter {

    private static final String NO_REQIP = "-";

    @Override
    public String convert(ILoggingEvent event) {
        String reqip = UserContextHolder.getReqip();
        return reqip == null ? NO_REQIP : reqip;
    }
}
