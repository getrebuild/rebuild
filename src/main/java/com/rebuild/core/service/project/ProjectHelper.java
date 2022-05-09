/*!
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

import java.util.Set;

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
            ProjectManager.instance.getProjectByX(taskOrComment, user);
            return true;
        } catch (ConfigurationException | AccessDeniedException ex) {
            return false;
        }
    }

    /**
     * 对 任务/评论/标签 是否有管理权
     *
     * @param taskOrCommentOrTag
     * @param user
     * @return
     */
    public static boolean isManageable(ID taskOrCommentOrTag, ID user) {
        // 管理员
        if (UserHelper.isAdmin(user)) return true;

        // 项目配置信息
        ConfigBean pcfg;
        if (taskOrCommentOrTag.getEntityCode() == EntityHelper.ProjectTaskTag) {
            Object[] projectId = Application.getQueryFactory().uniqueNoFilter(taskOrCommentOrTag, "projectId");
            pcfg = ProjectManager.instance.getProject((ID) projectId[0], null);
        } else {
            pcfg = ProjectManager.instance.getProjectByX(convert2Task(taskOrCommentOrTag), null);
        }
        
        // 负责人
        if (user.equals(pcfg.getID("principal"))) return true;
        // 非成员
        if (!pcfg.get("members", Set.class).contains(user)) return false;

        // 创建人
        Object[] createdBy = Application.getQueryFactory().uniqueNoFilter(taskOrCommentOrTag, "createdBy");
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
