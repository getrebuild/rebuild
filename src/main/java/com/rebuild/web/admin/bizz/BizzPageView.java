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
 * Bizz entity URL-Rewrite
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/app/")
public class BizzPageView extends EntityController {

    @GetMapping("User/view/{id}")
    public ModelAndView userView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/user-view", "User", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("User/list")
    public String userList() {
        return "redirect:/admin/bizuser/users";
    }

    @GetMapping("Department/view/{id}")
    public ModelAndView deptView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/dept-view", "Department", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Department/list")
    public String deptList() {
        return "redirect:/admin/bizuser/departments";
    }

    @GetMapping("Role/view/{id}")
    public ModelAndView roleView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/role-view", "Role", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Role/list")
    public String roleList() {
        return "redirect:/admin/bizuser/role-privileges";
    }

    @GetMapping("Team/view/{id}")
    public ModelAndView teamView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/team-view", "Team", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Team/list")
    public String teamList() {
        return "redirect:/admin/bizuser/teams";
    }
}
