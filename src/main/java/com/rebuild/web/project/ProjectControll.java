/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

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
        return null;
    }

    @RequestMapping("settings/post")
    public void settingsPorject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("settings/plan/post")
    public void settingsPorjectPlan(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("settings/delete")
    public void deletePorject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("settings/plan/delete")
    public void deletePorjectPlan(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
