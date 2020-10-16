/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.project;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.NoRecordFoundException;

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
            ConfigBean cfg = ProjectManager.instance.getProjectByTask(convert2Task(taskOrComment), null);
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
