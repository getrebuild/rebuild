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
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * 群组
 *
 * @author devezhao
 * @since 2019/11/9
 */
public class FeedsGroupHelper {

    /**
     * @param user
     * @param usesAll
     * @return
     * @see #findGroups(ID, boolean, boolean)
     */
    public static FeedsGroup[] findGroups(ID user, boolean usesAll) {
        return findGroups(user, usesAll, false);
    }

    /**
     * 获取可用群组
     *
     * @param user
     * @param usesAll 管理员是否返回全部
     * @param reload
     * @return
     */
    public static FeedsGroup[] findGroups(ID user, boolean usesAll, boolean reload) {
        ArrayList<FeedsGroup> groups = reload ? null
                : (ArrayList<FeedsGroup>) Application.getCommonCache().getx("FeedsGroupV1");
        if (groups == null) {
            Object[][] array = Application.createQueryNoFilter("select groupId,name,members from FeedsGroup").array();
            groups = new ArrayList<>();
            for (Object[] o : array) {
                groups.add(new FeedsGroup((ID) o[0], (String) o[1], (JSON) JSON.parse((String) o[2])));
            }
            Application.getCommonCache().putx("FeedsGroupV1", groups);
        }

        // 管理员返回全部
        if (usesAll && UserHelper.isAdmin(user)) {
            return groups.toArray(new FeedsGroup[0]);
        }

        List<FeedsGroup> inGroups = new ArrayList<>();
        for (FeedsGroup g : groups) {
            if (g.getUsers().contains(user)) {
                inGroups.add(g);
            }
        }
        return inGroups.toArray(new FeedsGroup[0]);
    }

    /**
     * @param groupId
     * @return
     */
    public static String getGroupName(ID groupId) {
        for (FeedsGroup g : findGroups(UserService.SYSTEM_USER, true)) {
            if (g.getId().equals(groupId)) {
                return g.getName();
            }
        }
        return FeedsScope.GROUP.getName();
    }
}
