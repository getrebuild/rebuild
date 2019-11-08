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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsGroup;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2019/11/8
 */
@Controller
@RequestMapping("/feeds/group/")
public class FeedsGroupControll extends BaseControll {

    @RequestMapping({ "list", "group-list" })
    public void groupList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        FeedsGroup[] groups = FeedsHelper.findGroups(user, false);

        JSONArray ret = new JSONArray();
        for (FeedsGroup g : groups) {
            JSONObject o = JSONUtils.toJSONObject(new String[] { "id", "name" }, new Object[] { g.getId(), g.getName() });
            if (UserHelper.isAdmin(user)) {
                o.put("members", g.getMembers());
            }
            ret.add(o);
        }
        writeSuccess(response, ret);
    }

    @RequestMapping("user-list")
    public void userList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONArray ret = new JSONArray();
        for (User u : Application.getUserStore().getAllUsers()) {
            JSONObject o = JSONUtils.toJSONObject(new String[] { "id", "name" }, new Object[] { u.getId(), u.getFullName() });
            ret.add(o);
        }
        writeSuccess(response, ret);
    }
}
