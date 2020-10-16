/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * URL-Rewrite
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/app/")
public class BizzPageView extends EntityController {

    @GetMapping("User/view/{id}")
    public ModelAndView userView(@PathVariable String id, HttpServletRequest request) {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/user-view", "User", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @GetMapping("Department/view/{id}")
    public ModelAndView deptView(@PathVariable String id, HttpServletRequest request) {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/dept-view", "Department", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @GetMapping("Role/view/{id}")
    public ModelAndView roleView(@PathVariable String id, HttpServletRequest request) {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-view", "Role", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @GetMapping("Team/view/{id}")
    public ModelAndView teamView(@PathVariable String id, HttpServletRequest request) {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/team-view", "Team", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }
}
