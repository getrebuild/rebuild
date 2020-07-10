/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

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
        mv.getModelMap().put("iconName", p.getString("iconName"));
        mv.getModelMap().put("projectCode", p.getString("projectCode"));
        mv.getModelMap().put("projectName", p.getString("projectName"));

        final ConfigEntry[] plans = ProjectManager.instance.getPlansOfProject(projectId2);
        JSONArray plans2 = new JSONArray();
        for (ConfigEntry e : plans) {
            plans2.add(e.toJSON());
        }
        mv.getModelMap().put("projectPlans", plans2.toJSONString());

        return mv;
    }

    // 项目 ID
    private static final Pattern PATT_CODE = Pattern.compile("[a-zA-Z]{2,6}");

    @RequestMapping("search")
    public ModelAndView searchProject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String gs = getParameter(request, "gs");
        // 全局搜索
        if (StringUtils.isNotBlank(gs)) {
            String[] codes = gs.split("-");
            Object[] project = null;
            if (codes.length == 2) {
                project = Application.createQuery(
                        "select projectId,taskId from ProjectTask where projectId.projectCode = ? or taskNumber = ?")
                        .setParameter(1, codes[0])
                        .setParameter(2, ObjectUtils.toLong(codes[1]))
                        .unique();
            } else if (PATT_CODE.matcher(codes[0]).matches()) {
                project = Application.createQuery(
                        "select configId from ProjectConfig where projectCode = ?")
                        .setParameter(1, codes[0])
                        .unique();
            }

            if (project != null) {
                String projectUrl = AppUtils.getContextPath() + "/project/" + project[0] + "/tasks";
                if (project.length == 2) {
                    projectUrl += "#!/View/ProjectTask/" + project[1];
                }

                response.sendRedirect(projectUrl);
                return null;
            }
        }

        return createModelAndView("/project/project-search.jsp");
    }
}
