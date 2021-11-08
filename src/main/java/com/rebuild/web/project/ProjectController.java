/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@RestController
@RequestMapping("/project/")
public class ProjectController extends BaseController {

    // 项目 ID
    private static final Pattern PATT_CODE = Pattern.compile("[a-zA-Z]{2,6}");

    @GetMapping("{projectId}/tasks")
    public ModelAndView pageProject(@PathVariable String projectId,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID projectId2 = ID.isId(projectId) ? ID.valueOf(projectId) : null;
        if (projectId2 == null) {
            response.sendError(404);
            return null;
        }

        final ID user = getRequestUser(request);

        ConfigBean p;
        try {
            p = ProjectManager.instance.getProject(projectId2, getRequestUser(request));
        } catch (ConfigurationException ex) {
            response.sendError(403, ex.getLocalizedMessage());
            return null;
        }

        ModelAndView mv = createModelAndView("/project/project-tasks");
        mv.getModel().put("projectId", p.getID("id"));
        mv.getModel().put("iconName", p.getString("iconName"));
        mv.getModel().put("projectCode", p.getString("projectCode"));
        mv.getModel().put("projectName", p.getString("projectName"));
        mv.getModel().put("isMember", p.get("members", Set.class).contains(user));
        mv.getModel().put("scope", p.getInteger("scope"));
        mv.getModel().put("status", p.getInteger("status"));

        final ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(projectId2);
        JSONArray plansList = new JSONArray();
        for (ConfigBean e : plans) {
            plansList.add(e.toJSON());
        }
        mv.getModel().put("projectPlans", plansList.toJSONString());

        return mv;
    }

    @GetMapping("{projectId}/details")
    public RespBody getPlans(@PathVariable String projectId, HttpServletRequest request) throws IOException {
        final ID user = getRequestUser(request);
        
        JSONObject details;
        try {
            ConfigBean p = ProjectManager.instance.getProject(ID.valueOf(projectId), user);
            details = JSONUtils.toJSONObject(
                    new String[] { "projectName", "isMember", "projectStatus" },
                    new Object[] { p.getString("projectName"), p.get("members", Set.class).contains(user), p.getInteger("status") });

        } catch (ConfigurationException ex) {
            return RespBody.error(ex.getLocalizedMessage(), 403);
        }

        ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(ID.valueOf(projectId));
        JSONArray array = new JSONArray();
        for (ConfigBean cb : plans) {
            array.add(cb.toJSON());
        }
        details.put("projectPlans", array);

        return RespBody.ok(details);
    }

    /**
     * @see com.rebuild.web.general.ListAndViewRedirection
     */
    @GetMapping("search")
    public void searchProject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String gs = getParameter(request, "gs");
        String baseUrl = AppUtils.getContextPath("/project/");

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
                return;
            }
        }

        // 未找到就跳转到第一个项目
        ConfigBean[] ccc = ProjectManager.instance.getAvailable(getRequestUser(request));
        if (ccc.length == 0) {
            response.sendError(404, Language.L("没有可用项目"));
        } else {
            String projectUrl = baseUrl + ccc[0].getID("id") + "/tasks#gs=";
            if (gs != null) projectUrl += CodecUtils.urlEncode(gs);
            response.sendRedirect(projectUrl);
        }
    }
}
