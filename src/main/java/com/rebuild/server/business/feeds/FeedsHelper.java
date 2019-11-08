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

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO 缓存
 *
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelper {

    /**
     * @param source
     * @return
     */
    public static int getNumOfLike(ID source) {
        Object[] c = Application.createQueryNoFilter(
                "select count(likeId) from FeedsLike where source = ?")
                .setParameter(1, source)
                .unique();
        return ObjectUtils.toInt(c[0]);
    }

    /**
     * @param feedsId
     * @return
     */
    public static int getNumOfComment(ID feedsId) {
        Object[] c = Application.createQueryNoFilter(
                "select count(commentId) from FeedsComment where feedsId = ?")
                .setParameter(1, feedsId)
                .unique();
        return ObjectUtils.toInt(c[0]);
    }

    /**
     * @param content
     * @return
     *
     * @see #findMentionsMap(String)
     */
    public static ID[] findMentions(String content) {
        Set<ID> set = new HashSet<>(findMentionsMap(content).values());
        return set.toArray(new ID[0]);
    }

    /**
     * 获取内容中的 @USERID
     *
     * @param content
     * @return
     */
    public static Map<String, ID> findMentionsMap(String content) {
        Map<String, ID> found = new HashMap<>();
        for (String ats : content.split("@")) {
            if (StringUtils.isBlank(ats)) continue;
            String[] atss = ats.split(" ");

            String fullName = atss[0];
            ID user = UserHelper.findUserByFullName(fullName);
            if (user == null && atss.length >= 2) {
                fullName = atss[0] + " " + atss[1];
                user = UserHelper.findUserByFullName(fullName);

                if (user == null && atss.length >= 3) {
                    fullName = atss[0] + " " + atss[1] + " " + atss[2];
                    user = UserHelper.findUserByFullName(fullName);
                }
            }

            if (user != null) {
                found.put(fullName, user);
            }
        }
        return found;
    }

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
