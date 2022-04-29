/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 通用日志保存
 *
 * @author RB
 * @since 2022/4/27
 */
public class CommonsLog {

    public static final int STATUS_OK = 1;  // def
    public static final int STATUS_ERROR = 2;

    public static final String TYPE_TRIGGER = "TRIGGER";
    public static final String TYPE_EXPORT = "EXPORT";

    public static void createLog(String type, ID user, ID source) {
        createLog(type, user, source, null, STATUS_OK);
    }

    public static void createLog(String type, ID user, ID source, String content) {
        createLog(type, user, source, content, STATUS_OK);
    }

    public static void createLog(String type, ID user, ID source, Throwable error) {
        createLog(type, user, source, error.getLocalizedMessage(), STATUS_ERROR);
    }

    public static void createLog(String type, ID user, ID source, String content, int status) {
        Record log = EntityHelper.forNew(EntityHelper.CommonsLog, user);
        log.setString("type", type);
        log.setID("user", user);
        log.setID("source", ObjectUtils.defaultIfNull(source, user));
        log.setInt("status", status);
        log.setDate("logTime", CalendarUtils.now());
        if (content != null) log.setString("logContent", content);

        ThreadPool.exec(() -> Application.getCommonsService().create(log));
    }
}
