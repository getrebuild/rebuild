/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import com.rebuild.server.Application;
import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

    @RequestMapping("/admin/project/{id}")
    public ModelAndView pageEditor(@PathVariable String id) throws IOException {
        return createModelAndView("/admin/project/project-editor.jsp");
    }

    @RequestMapping("/admin/projects/list")
    public void list(HttpServletResponse resp) throws IOException {
        Object[][] array = Application.createQuery(
                "select configId,projectName,projectCode from ProjectConfig order by projectName")
                .array();
        writeSuccess(resp, array);
    }
}
