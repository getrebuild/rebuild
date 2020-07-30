/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ProjectManager;
import com.rebuild.server.helper.ConfigurationException;
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
import java.util.Set;
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

    // 项目 ID
    private static final Pattern PATT_CODE = Pattern.compile("[a-zA-Z]{2,6}");

    @RequestMapping("{projectId}/tasks")
    public ModelAndView pageProject(@PathVariable String projectId,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final ID projectId2 = ID.isId(projectId) ? ID.valueOf(projectId) : null;
        if (projectId2 == null) {
            response.sendError(404);
            return null;
        }


        ConfigEntry p;
        try {
            p = ProjectManager.instance.getProject(projectId2, getRequestUser(request));
        } catch (ConfigurationException ex) {
            response.sendError(404, ex.getLocalizedMessage());
            return null;
        }

        ModelAndView mv = createModelAndView("/project/project-tasks.jsp");
        mv.getModel().put("projectId", p.getID("id"));
        mv.getModel().put("iconName", p.getString("iconName"));
        mv.getModel().put("projectCode", p.getString("projectCode"));
        mv.getModel().put("projectName", p.getString("projectName"));
        mv.getModel().put("isMember", p.get("members", Set.class).contains(user));

        final ConfigEntry[] plans = ProjectManager.instance.getPlansOfProject(projectId2);
        JSONArray plansList = new JSONArray();
        for (ConfigEntry e : plans) {
            plansList.add(e.toJSON());
        }
        mv.getModel().put("projectPlans", plansList.toJSONString());

        return mv;
    }

    @RequestMapping("search")
    public ModelAndView searchProject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String gs = getParameter(request, "gs");
        String baseUrl = AppUtils.getContextPath() + "/project/";

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
                String projectUrl = baseUrl + project[0] + "/tasks";
                if (project.length == 2) {
                    projectUrl += "#!/View/ProjectTask/" + project[1];
                }

                response.sendRedirect(projectUrl);
                return null;
            }
        }

        // 未找到就跳转到第一个项目
        ConfigEntry[] ee = ProjectManager.instance.getAvailable(getRequestUser(request));
        if (ee.length == 0) {
            response.sendError(404, "没有可用项目");
        } else {
            String projectUrl = baseUrl + ee[0].getID("id") + "/tasks#gs=" + CodecUtils.urlEncode(gs);
            response.sendRedirect(projectUrl);
        }
        return null;
    }
}
