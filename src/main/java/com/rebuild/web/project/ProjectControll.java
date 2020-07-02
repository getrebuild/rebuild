/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 项目设置
 *
 * @author devezhao
 * @since 2020/6/29
 */
@RequestMapping("/project")
@Controller
public class ProjectControll extends BasePageControll {

    @RequestMapping("{projectId}/tasks")
    public ModelAndView pageProject(@PathVariable String projectId,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID projectId2 = ID.isId(projectId) ? ID.valueOf(projectId) : null;
        if (projectId2 == null) {
            response.sendError(404);
            return null;
        }

        final ConfigEntry p = ProjectManager.instance.getProject(projectId2, getRequestUser(request));
        ModelAndView mv = createModelAndView("/project/project-tasks.jsp");
        mv.getModelMap().put("projectId", p.getID("id"));
        mv.getModelMap().put("projectCode", p.getString("projectCode"));
        mv.getModelMap().put("projectName", p.getString("projectName"));

        final ConfigEntry[] plans = ProjectManager.instance.getPlanList(projectId2);
        JSONArray plans2 = new JSONArray();
        for (ConfigEntry e : plans) {
            plans2.add(e.toJSON());
        }
        mv.getModelMap().put("projectPlans", plans2.toJSONString());

        return mv;
    }
}
