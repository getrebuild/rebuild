/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.project.ProjectConfigService;
import com.rebuild.core.service.project.ProjectPlanConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/30
 */
@RestController
@RequestMapping("/admin/")
public class ProjectAdminController extends BaseController {

    @GetMapping("projects")
    public ModelAndView pageList() {
        return createModelAndView("/admin/project/project-list");
    }

    @GetMapping("project/{projectId}")
    public ModelAndView pageEditor(@PathVariable String projectId, HttpServletResponse response) throws IOException {
        ID projectId2 = ID.isId(projectId) ? ID.valueOf(projectId) : null;
        if (projectId2 == null) {
            response.sendError(404);
            return null;
        }

        Object[] p = Application.createQuery(
                "select projectName,scope,principal,members,extraDefinition,status from ProjectConfig where configId = ?")
                .setParameter(1, projectId2)
                .unique();

        ModelAndView mv = createModelAndView("/admin/project/project-editor");
        mv.getModelMap().put("projectName", p[0]);
        mv.getModelMap().put("scope", p[1]);
        mv.getModelMap().put("principal", p[2]);
        mv.getModelMap().put("members", p[3]);
        mv.getModelMap().put("extraDefinition", p[4]);
        mv.getModelMap().put("status", ObjectUtils.toInt(p[5], 1));
        return mv;
    }

    @GetMapping("projects/list")
    public Object listProjects() {
        return Application.createQuery(
                "select configId,projectName,projectCode,iconName,status from ProjectConfig order by status asc, projectName")
                .array();
    }

    @GetMapping("projects/plan-list")
    public Object listPlans(@IdParam(name = "project") ID projectId) {
        return Application.createQuery(
                "select configId,planName,flowStatus,flowNexts,seq from ProjectPlanConfig where projectId = ? order by seq")
                .setParameter(1, projectId)
                .array();
    }

    @PostMapping("projects/post")
    public RespBody postProject(HttpServletRequest req) {
        final ID user = getRequestUser(req);
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(req);

        int useTemplate = 0;
        if (data.containsKey("_useTemplate")) {
            useTemplate = (int) data.remove("_useTemplate");
        }

        Record project = EntityHelper.parse(data, user);
        if (project.getPrimary() == null) {
            String projectCode = project.getString("projectCode");
            Object exists = Application.createQuery(
                    "select projectCode from ProjectConfig where projectCode = ?")
                    .setParameter(1, projectCode)
                    .unique();
            if (exists != null) {
                return RespBody.errorl("项目 ID 重复");
            }

            project = Application.getBean(ProjectConfigService.class).createProject(project, useTemplate);

        } else {
            Application.getBean(ProjectPlanConfigService.class).update(project);
        }

        return RespBody.ok(JSONUtils.toJSONObject("id", project.getPrimary()));
    }
}
