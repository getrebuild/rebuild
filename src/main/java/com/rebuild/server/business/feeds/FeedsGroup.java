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

package com.rebuild.server.business.feeds;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.service.bizz.UserHelper;

import java.util.Set;

/**
 * 群组
 *
 * @author devezhao
 * @since 2019/11/8
 */
public class FeedsGroup {

    private ID id;
    private String name;
    private JSON members;

    private Set<ID> users;

    protected FeedsGroup(ID id, String name, JSON members) {
        this.id = id;
        this.name = name;
        this.members = members;
    }

    public ID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public JSON getMembers() {
        return members;
    }

    public Set<ID> getUsers() {
        if (users != null) return users;
        users = UserHelper.parseUsers((JSONArray) this.members, null);
        return users;
    }
}
