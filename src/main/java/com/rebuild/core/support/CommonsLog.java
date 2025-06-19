/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
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
    public static final String TYPE_REPORT = "REPORT";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_EXTFORM = "EXTFORM";
    public static final String TYPE_TRANSFORM = "TRANSFORM";

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
        // v4.1 关闭触发器日志
        if (source != null && source.getEntityCode() == EntityHelper.RobotTriggerConfig) {
            String triggerName = FieldValueHelper.getLabelNotry(source);
            if (triggerName.contains("关闭日志")) return;
        }

        Record commLog = EntityHelper.forNew(EntityHelper.CommonsLog, user);
        commLog.setString("type", type);
        commLog.setID("user", user);
        commLog.setID("source", ObjectUtils.defaultIfNull(source, user));
        commLog.setInt("status", status);
        commLog.setDate("logTime", CalendarUtils.now());
        if (content != null) commLog.setString("logContent", CommonsUtils.maxstr(content, 32767));

        TaskExecutors.queue(() -> Application.getCommonsService().create(commLog, false));
    }

    /**
     * 记录转换日志
     *
     * @param user
     * @param sourceId
     * @param targetId
     * @param transformId
     */
    public static void createTransformLog(ID user, ID sourceId, ID targetId, ID transformId) {
        JSON content = JSONUtils.toJSONObject(
                new String[]{"transform", "source"},
                new Object[]{transformId, sourceId});
        createLog(TYPE_TRANSFORM, user, targetId, content.toJSONString(), STATUS_OK);
    }
}
