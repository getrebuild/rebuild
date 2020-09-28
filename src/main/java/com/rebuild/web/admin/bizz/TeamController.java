/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.TeamService;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/13
 */
@Controller
@RequestMapping("/admin/bizuser/")
public class TeamController extends EntityController {

    @GetMapping("teams")
    public ModelAndView pageList(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ModelAndView mv = createModelAndView("/admin/bizuser/team-list", "Team", user);
        JSON config = DataListManager.instance.getFieldsLayout("Team", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @GetMapping("team-members")
    public void getMembers(HttpServletRequest request, HttpServletResponse response) {
        ID teamId = getIdParameterNotNull(request, "team");
        Team team = Application.getUserStore().getTeam(teamId);

        List<Object[]> members = new ArrayList<>();
        for (Principal p : team.getMembers()) {
            User user = (User) p;
            members.add(new Object[]{
                    user.getId(), user.getFullName(),
                    user.getOwningDept() != null ? user.getOwningDept().getName() : null
            });
        }
        writeSuccess(response, members);
    }

    @PostMapping("team-members-add")
    public void addMembers(HttpServletRequest request, HttpServletResponse response) {
        final ID teamId = getIdParameterNotNull(request, "team");

        JSON usersDef = ServletUtils.getRequestJson(request);
        Set<ID> users = UserHelper.parseUsers((JSONArray) usersDef, null);

        if (!users.isEmpty()) {
            Application.getBean(TeamService.class).createMembers(teamId, users);
        }
        writeSuccess(response);
    }

    @PostMapping("team-members-del")
    public void deleteMembers(HttpServletRequest request, HttpServletResponse response) {
        ID teamId = getIdParameterNotNull(request, "team");
        ID userId = getIdParameterNotNull(request, "user");

        Application.getBean(TeamService.class).deleteMembers(teamId, Collections.singletonList(userId));
        writeSuccess(response);
    }
}
