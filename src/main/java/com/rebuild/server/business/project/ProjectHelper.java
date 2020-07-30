/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.project;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsScope;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.metadata.EntityHelper;

import java.awt.print.PrinterAbortException;

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
        // 转为任务 ID
        if (taskOrComment.getEntityCode() == EntityHelper.ProjectTaskComment) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(taskOrComment, "taskId");
            if (o == null) return false;
            taskOrComment = (ID) o[0];
        }

        try {
            // 能访问就有读取权限
            ProjectManager.instance.getProjectByTask(taskOrComment, user);
            return true;
        } catch (ConfigurationException | PrivilegesException ex) {
            return false;
        }
    }
}
