/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.configuration.ProjectConfigService;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/30
 */
@Controller
public class ProjectAdminControll extends BasePageControll {

    @RequestMapping("/admin/projects")
    public ModelAndView pageList() throws IOException {
        return createModelAndView("/admin/project/project-list.jsp");
    }

    @RequestMapping("/admin/project/{projectId}")
    public ModelAndView pageEditor(@PathVariable String projectId, HttpServletResponse response) throws IOException {
        ID projectId2 = ID.isId(projectId) ? ID.valueOf(projectId) : null;
        if (projectId2 == null) {
            response.sendError(404, "无效的项目 ID");
            return null;
        }

        Object[] p = Application.createQuery(
                "select projectName,principal,members from ProjectConfig where configId = ?")
                .setParameter(1, projectId2)
                .unique();

        ModelAndView mv = createModelAndView("/admin/project/project-editor.jsp");
        mv.getModelMap().put("projectName", p[0]);
        mv.getModelMap().put("principal", p[1]);
        mv.getModelMap().put("members", p[2]);

        return mv;
    }

    @RequestMapping("/admin/projects/list")
    public void listProjects(HttpServletResponse resp) throws IOException {
        Object[][] array = Application.createQuery(
                "select configId,projectName,projectCode from ProjectConfig order by projectName")
                .array();
        writeSuccess(resp, array);
    }

    @RequestMapping("/admin/projects/plan-list")
    public void listPlans(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ID projectId = getIdParameterNotNull(req, "project");
        Object[][] array = Application.createQuery(
                "select configId,planName,comments from ProjectPlanConfig where projectId = ? order by seq")
                .setParameter(1, projectId)
                .array();
        writeSuccess(resp, array);
    }

    @RequestMapping("/admin/projects/updates")
    public void saveProject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final ID user = getRequestUser(req);
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(req);

        // 面板排序
        List<Record> plans = new ArrayList<>();
        String sortPlans = (String) data.remove("_sorts");
        if (StringUtils.isNotBlank(sortPlans)) {
            int seq = 1;
            for (String o : sortPlans.split(">")) {
                Record plan = EntityHelper.forUpdate(ID.valueOf(o), user);
                plan.setInt("seq", seq++);
                plans.add(plan);
            }
        }

        Record project = EntityHelper.parse(data, user);
        Application.getBean(ProjectConfigService.class).updateProjectAndPlans(project, plans.toArray(new Record[0]));

        writeSuccess(resp);
    }
}
