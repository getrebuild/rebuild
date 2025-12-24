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
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
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
    @GetMapping({"/app/list-and-view", "/app/redirect"})
    public void redirect(@IdParam ID anyId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = null;

        if (MetadataHelper.containsEntity(anyId.getEntityCode())) {
            final String type = getParameter(request, "type");
            final Entity entity = MetadataHelper.getEntity(anyId.getEntityCode());

            if (entity.getEntityCode() == EntityHelper.Feeds) {
                url = "../feeds/home#s=" + anyId;

            } else if (entity.getEntityCode() == EntityHelper.FeedsComment) {
                ID found = findFeedsId(anyId);
                if (found != null) url = "../feeds/home#s=" + found;

            } else if (entity.getEntityCode() == EntityHelper.ProjectTask
                    || entity.getEntityCode() == EntityHelper.ProjectTaskComment) {
                Object[] found = findProjectAndTaskId(anyId);
                if (found != null) {
                    url = MessageFormat.format("../project/{0}/tasks#!/View/ProjectTask/{1}", found[1], found[0]);
                }

            } else if (entity.getEntityCode() == EntityHelper.User) {
                url = "newtab".equalsIgnoreCase(type)
                        ? String.format("User/view/%s", anyId)
                        : String.format("../admin/bizuser/users#!/View/User/%s", anyId);

            } else if (entity.getEntityCode() == EntityHelper.Department) {
                url = "newtab".equalsIgnoreCase(type)
                        ? String.format("Department/view/%s", anyId)
                        : String.format("../admin/bizuser/departments#!/View/Department/%s", anyId);

            } else if (entity.getEntityCode() == EntityHelper.Team) {
                url = String.format("../admin/bizuser/teams#!/View/Team/%s", anyId);

            } else if (entity.getEntityCode() == EntityHelper.Role) {
                url = String.format("../admin/bizuser/role/%s", anyId);

            } else if (MetadataHelper.isBusinessEntity(entity)) {
                if ("dock".equalsIgnoreCase(type)) {
                    url = String.format("entity/view?id=%s", anyId);
                } else if ("newtab".equalsIgnoreCase(type)) {
                    url = String.format("%s/view/%s", entity.getName(), anyId);
                } else {
                    url = MessageFormat.format("{0}/list#!/View/{0}/{1}", entity.getName(), anyId);
                }
            }
        }

        if (url != null) response.sendRedirect(url);
        else response.sendError(HttpStatus.NOT_FOUND.value());
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

    @GetMapping("/app/entity/view")
    public ModelAndView dockView(@IdParam ID recordId) {
        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        String viewUrl = String.format("../%s/view/%s", entity.getName(), recordId);

        ModelAndView mv = createModelAndView("/general/dock-view");
        mv.getModel().put("entityName", entity.getName());
        mv.getModel().put("entityLabel", EasyMetaFactory.getLabel(entity));
        mv.getModel().put("id", recordId);
        mv.getModel().put("title", Language.L("%s详情", EasyMetaFactory.getLabel(entity)));
        mv.getModel().put("viewUrl", viewUrl);
        return mv;
    }

    @GetMapping("/app/entity/form")
    public ModelAndView dockForm(HttpServletRequest request) {
        final String idOrEntity = StringUtils.defaultString(
                getParameter(request, "id"), getParameter(request, "e"));

        Entity entity;
        ID id = null;
        if (ID.isId(idOrEntity)) {
            id = ID.valueOf(idOrEntity);
            entity = MetadataHelper.getEntity(id.getEntityCode());
        } else {
            entity = MetadataHelper.getEntity(idOrEntity);
        }

        ModelAndView mv = createModelAndView("/general/dock-form");
        EasyEntity easyMeta = EasyMetaFactory.valueOf(entity);
        mv.getModel().put("entityName", easyMeta.getName());
        mv.getModel().put("entityLabel", easyMeta.getLabel());
        mv.getModel().put("entityIcon", easyMeta.getIcon());
        mv.getModel().put("id", id);
        mv.getModel().put("title", id == null
                ? Language.L("新建%s", easyMeta.getLabel())
                : Language.L("编辑%s", easyMeta.getLabel()));
        return mv;
    }
}
