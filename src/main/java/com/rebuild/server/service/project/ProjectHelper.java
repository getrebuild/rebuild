/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.project;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;

/**
 * @author devezhao
 * @since 2020/7/30
 */
public class ProjectHelper {

    /**
     * 用户对指定项目是否可读
     *
     * @param taskOrComment
     * @param user
     * @return
     */
    public static boolean checkReadable(ID taskOrComment, ID user) {
        try {
            taskOrComment = convert2Task(taskOrComment);

            // 能访问就有读取权限
            ProjectManager.instance.getProjectByTask(taskOrComment, user);
            return true;
        } catch (ConfigurationException | AccessDeniedException ex) {
            return false;
        }
    }

    /**
     * 对任务/评论是否有管理权
     *
     * @param taskOrComment
     * @param user
     * @return
     */
    public static boolean isManageable(ID taskOrComment, ID user) {
        if (taskOrComment.getEntityCode() == EntityHelper.ProjectTask) {
            // 管理员
            if (UserHelper.isAdmin(user)) return true;

            // 负责人
            ConfigEntry cfg = ProjectManager.instance.getProjectByTask(convert2Task(taskOrComment), null);
            if (user.equals(cfg.getID("principal"))) return true;
        }

        // 创建人
        Object[] createdBy = Application.getQueryFactory().uniqueNoFilter(taskOrComment, "createdBy");
        return createdBy != null && createdBy[0].equals(user);
    }

    // 转为任务 ID
    private static ID convert2Task(ID taskOrComment) {
        if (taskOrComment.getEntityCode() == EntityHelper.ProjectTaskComment) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(taskOrComment, "taskId");
            if (o == null) {
                throw new NoRecordFoundException(taskOrComment);
            }

            return (ID) o[0];
        }
        return taskOrComment;
    }
}
