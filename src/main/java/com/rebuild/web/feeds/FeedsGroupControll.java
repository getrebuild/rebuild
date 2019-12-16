/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        ID user = getRequestUser(request);
        Set<Team> teams = Application.getUserStore().getUser(user).getOwningTeams();

        JSONArray ret = new JSONArray();
        for (Team t : teams) {
            JSONObject o = JSONUtils.toJSONObject(
                    new String[] { "id", "name" }, new Object[] { t.getIdentity(), t.getName() });
            ret.add(o);
        }
        writeSuccess(response, ret);
    }

    @RequestMapping("user-list")
    public void userList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONArray ret = new JSONArray();
        for (User u : UserHelper.sortUsers()) {
            JSONObject o = JSONUtils.toJSONObject(
                    new String[] { "id", "name" },
                    new Object[] { u.getId(), u.getFullName() });
            ret.add(o);
        }
        writeSuccess(response, ret);
    }
}
