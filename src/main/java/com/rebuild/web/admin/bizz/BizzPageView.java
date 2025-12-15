/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.MemberGroup;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.CombinedRole;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 * Bizz entity URL-Rewrite
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Slf4j
@RestController
@RequestMapping("/app/")
public class BizzPageView extends EntityController {

    @GetMapping("User/view/{id}")
    public ModelAndView userView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/user-view", "User", getRequestUser(request));
        mv.getModel().put("id", id);
        mv.getModel().put("serviceMail", SMSender.availableMail());
        return mv;
    }

    @GetMapping("User/list")
    public void userList(HttpServletResponse response) throws IOException {
        response.sendRedirect(AppUtils.getContextPath("/admin/bizuser/users"));
    }

    @GetMapping("Department/view/{id}")
    public ModelAndView deptView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/dept-view", "Department", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Department/list")
    public void deptList(HttpServletResponse response) throws IOException {
        response.sendRedirect(AppUtils.getContextPath("/admin/bizuser/departments"));
    }

    @GetMapping("Role/view/{id}")
    public ModelAndView roleView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/role-view", "Role", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Role/list")
    public void roleList(HttpServletResponse response) throws IOException {
        response.sendRedirect(AppUtils.getContextPath("/admin/bizuser/role-privileges"));
    }

    @GetMapping("Team/view/{id}")
    public ModelAndView teamView(@PathVariable ID id, HttpServletRequest request) {
        ModelAndView mv = createModelAndView(
                "/admin/bizuser/team-view", "Team", getRequestUser(request));
        mv.getModel().put("id", id);
        return mv;
    }

    @GetMapping("Team/list")
    public void teamList(HttpServletResponse response) throws IOException {
        response.sendRedirect(AppUtils.getContextPath("/admin/bizuser/teams"));
    }

    // -- GROUP

    @GetMapping("/bizuser/group-members")
    public JSON getMembers(@IdParam ID groupId) {
        JSONArray res = new JSONArray();

        if (groupId.getEntityCode() == EntityHelper.Role) {
            for (User user : Application.getUserStore().getAllUsers()) {
                if (user.getId().equals(UserService.SYSTEM_USER)) continue;
                Role role = user.getOwningRole();
                if (role == null) continue;

                if (role.getIdentity().equals(groupId)) {
                    res.add(new Object[]{
                            user.getId(),
                            user.getFullName(),
                            user.getOwningDept() != null ? user.getOwningDept().getName() : null,
                            user.isActive(),
                    });
                } else if (role instanceof CombinedRole) {
                    if (((CombinedRole) role).getRoleAppends().contains(groupId)) {
                        res.add(new Object[]{
                                user.getId(),
                                user.getFullName(),
                                user.getOwningDept() != null ? user.getOwningDept().getName() : null,
                                user.isActive(),
                                true
                        });
                    }
                }
            }
            return res;
        }

        MemberGroup group;
        if (groupId.getEntityCode() == EntityHelper.Department) {
            group = Application.getUserStore().getDepartment(groupId);
        } else if (groupId.getEntityCode() == EntityHelper.Team) {
            group = Application.getUserStore().getTeam(groupId);
        } else {
            log.warn("No group defined : {}", groupId);
            return JSONUtils.EMPTY_ARRAY;
        }

        for (Principal p : group.getMembers()) {
            User user = (User) p;
            if (user.getId().equals(UserService.SYSTEM_USER)) continue;

            res.add(new Object[]{
                    user.getId(),
                    user.getFullName(),
                    user.getOwningDept() != null ? user.getOwningDept().getName() : null,
                    user.isActive()
            });
        }
        return res;
    }
}
