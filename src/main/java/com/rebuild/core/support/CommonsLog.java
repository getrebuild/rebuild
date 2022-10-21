/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.task.TaskExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 通用日志记录
 *
 * @author RB
 * @since 2022/4/27
 */
@Slf4j
public class CommonsLog {

    public static final int STATUS_OK = 1;  // default
    public static final int STATUS_ERROR = 2;

    public static final String TYPE_TRIGGER = "TRIGGER";
    public static final String TYPE_EXPORT = "EXPORT";

    /**
     * @param type
     * @param user
     * @param source
     * @param content
     */
    public static void createLog(String type, ID user, ID source, String content) {
        createLog(type, user, source, content, STATUS_OK);
    }

    /**
     * @param type
     * @param user
     * @param source
     * @param error
     */
    public static void createLog(String type, ID user, ID source, Throwable error) {
        createLog(type, user, source, error.getLocalizedMessage(), STATUS_ERROR);
    }

    /**
     * @param type
     * @param user
     * @param source
     * @param content
     * @param status
     */
    public static void createLog(String type, ID user, ID source, String content, int status) {
        Record clog = EntityHelper.forNew(EntityHelper.CommonsLog, user);
        clog.setString("type", type);
        clog.setID("user", user);
        clog.setID("source", ObjectUtils.defaultIfNull(source, user));
        clog.setInt("status", status);
        clog.setDate("logTime", CalendarUtils.now());
        if (content != null) clog.setString("logContent", content);

        TaskExecutors.queue(() -> Application.getCommonsService().create(clog, false));
    }
}
