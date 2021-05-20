/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * 群组 & 团队
 *
 * @author devezhao
 * @see Team
 * @since 2019/11/8
 */
@RestController
@RequestMapping("/feeds/group/")
public class FeedsGroupController extends BaseController {

    @GetMapping("group-list")
    public JSON groupList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");
        Set<Team> teams = Application.getUserStore().getUser(user).getOwningTeams();

        JSONArray ret = new JSONArray();
        for (Team t : teams) {
            if (StringUtils.isBlank(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                JSONObject o = JSONUtils.toJSONObject(
                        new String[]{"id", "name"}, new Object[]{t.getIdentity(), t.getName()});
                ret.add(o);
                if (ret.size() >= 20) break;
            }
        }
        return ret;
    }

    @GetMapping("user-list")
    public JSON userList(HttpServletRequest request) {
        final String query = getParameter(request, "q");

        JSONArray ret = new JSONArray();
        for (User u : UserHelper.sortUsers()) {
            if (StringUtils.isBlank(query)
                    || StringUtils.containsIgnoreCase(u.getFullName(), query)
                    || (u.getEmail() != null && StringUtils.containsIgnoreCase(u.getEmail(), query))) {
                JSONObject o = JSONUtils.toJSONObject(
                        new String[]{"id", "name"},
                        new Object[]{u.getId(), u.getFullName()});
                ret.add(o);
                if (ret.size() >= 20) break;
            }
        }
        return ret;
    }
}
