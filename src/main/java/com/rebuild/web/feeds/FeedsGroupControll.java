/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * 群组 & 团队
 *
 * @author devezhao
 * @since 2019/11/8
 *
 * @see Team
 */
@Controller
@RequestMapping("/feeds/group/")
public class FeedsGroupControll extends BaseControll {

    @RequestMapping("group-list")
    public void groupList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");
        Set<Team> teams = Application.getUserStore().getUser(user).getOwningTeams();

        JSONArray ret = new JSONArray();
        for (Team t : teams) {
            if (StringUtils.isBlank(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                JSONObject o = JSONUtils.toJSONObject(
                        new String[] { "id", "name" }, new Object[] { t.getIdentity(), t.getName() });
                ret.add(o);
                if (ret.size() >= 20) {
                    break;
                }
            }
        }
        writeSuccess(response, ret);
    }

    @RequestMapping("user-list")
    public void userList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String query = getParameter(request, "q");

        JSONArray ret = new JSONArray();
        for (User u : UserHelper.sortUsers()) {
            if (StringUtils.isBlank(query)
                    || StringUtils.containsIgnoreCase(u.getFullName(), query)
                    || (u.getEmail() != null && StringUtils.containsIgnoreCase(u.getEmail(), query))) {
                JSONObject o = JSONUtils.toJSONObject(
                        new String[] { "id", "name" },
                        new Object[] { u.getId(), u.getFullName() });
                ret.add(o);
                if (ret.size() >= 20) {
                    break;
                }
            }
        }
        writeSuccess(response, ret);
    }
}
