/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * 列表落地页跳转
 *
 * @author ZHAO
 * @since 2020/7/28
 */
@Controller
public class ListAndViewRedirection extends BaseController {

    // compatible: v3.1 "/app/list-and-view"
    @GetMapping({ "/app/list-and-view", "/app/redirect" })
    public void redirect(@IdParam ID anyId, HttpServletResponse response) throws IOException {
        String url = null;

        if (MetadataHelper.containsEntity(anyId.getEntityCode())) {
            Entity entity = MetadataHelper.getEntity(anyId.getEntityCode());

            if (entity.getEntityCode() == EntityHelper.Feeds) {
                url = "../feeds/home#s=" + anyId;

            } else if (entity.getEntityCode() == EntityHelper.FeedsComment) {
                ID found = findFeedsId(anyId);
                if (found != null) url = "../feeds/home#s=" + found;

            } else if (entity.getEntityCode() == EntityHelper.ProjectTask
                    || entity.getEntityCode() == EntityHelper.ProjectTaskComment) {
                Object[] found = findProjectAndTaskId(anyId);
                if (found != null) {
                    url = MessageFormat.format(
                            "../project/{0}/tasks#!/View/ProjectTask/{1}", found[1], found[0]);
                }

            } else if (entity.getEntityCode() == EntityHelper.User) {
                url = MessageFormat.format(
                        "../admin/bizuser/users#!/View/{0}/{1}", entity.getName(), anyId);

            } else if (MetadataHelper.isBusinessEntity(entity)) {
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
            return Application.getQueryFactory().uniqueNoFilter(taskOrComment, "taskId", "projectId");
        } else {
            return Application.getQueryFactory().uniqueNoFilter(taskOrComment, "taskId", "taskId.projectId");
        }
    }

    private ID findFeedsId(ID commentId) {
        Object[] feeds = Application.getQueryFactory().uniqueNoFilter(
                commentId, "feedsId");
        return feeds == null ? null : (ID) feeds[0];
    }
}
