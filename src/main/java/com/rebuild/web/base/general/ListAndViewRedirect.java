/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.base.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.web.BaseControll;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author ZHAO
 * @since 2020/7/28
 */
@Controller
public class ListAndViewRedirect extends BaseControll {

    @RequestMapping("/app/list-and-view")
    public void quickPageList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID anyId = getIdParameterNotNull(request, "id");

        String url = null;
        if (MetadataHelper.containsEntity(anyId.getEntityCode())) {
            Entity entity = MetadataHelper.getEntity(anyId.getEntityCode());

            if (entity.getEntityCode() == EntityHelper.Feeds) {
                url = "../feeds/home#s=" + anyId;
            } else if (entity.getEntityCode() == EntityHelper.FeedsComment) {
                ID found = findFeedsId(anyId);
                if (found != null) url = "../feeds/home#s=" + found;
            }  else if (entity.getEntityCode() == EntityHelper.ProjectTask
                    || entity.getEntityCode() == EntityHelper.ProjectTaskComment) {
                Object[] found = findProjectAndTaskId(anyId);
                if (found != null) {
                    url = MessageFormat.format(
                            "../project/{0}/tasks#!/View/ProjectTask/{1}", found[1], found[0]);
                }
            } else if (entity.getEntityCode() == EntityHelper.User) {
                url = MessageFormat.format(
                        "../admin/bizuser/users#!/View/{0}/{1}", entity.getName(), anyId);
            } else if (MetadataHelper.hasPrivilegesField(entity)
                    || EasyMeta.valueOf(anyId.getEntityCode()).isPlainEntity()) {
                url = MessageFormat.format("{0}/list#!/View/{0}/{1}", entity.getName(), anyId);
            }
        }

        if (url != null) {
            response.sendRedirect(url);
        } else {
            response.sendError(HttpStatus.NOT_FOUND.value());
        }
    }

    private Object[] findProjectAndTaskId(ID taskOrComment) {
        if (taskOrComment.getEntityCode() == EntityHelper.ProjectTask) {
            return Application.getQueryFactory().uniqueNoFilter(
                    taskOrComment, "taskId", "projectId");
        } else {
            return Application.getQueryFactory().uniqueNoFilter(
                    taskOrComment, "taskId", "taskId.projectId");
        }
    }

    private ID findFeedsId(ID commentId) {
        Object[] feeds = Application.getQueryFactory().uniqueNoFilter(
                commentId, "feedsId");
        return feeds == null ? null : (ID) feeds[0];
    }
}
