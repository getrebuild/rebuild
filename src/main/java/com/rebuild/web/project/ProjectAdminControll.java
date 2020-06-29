/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import com.rebuild.web.BasePageControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * 项目管理
 *
 * @author devezhao
 * @since 2020/6/30
 */
@RequestMapping("/admin/project/")
@Controller
public class ProjectAdminControll extends BasePageControll {

    @RequestMapping("manager")
    public ModelAndView pageList() throws IOException {
        return createModelAndView("/admin/project/project-list.jsp");
    }

}
