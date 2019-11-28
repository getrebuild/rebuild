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

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelper {

    /**
     * 评论数
     * TODO 缓存
     *
     * @param feedsId
     * @return
     */
    public static int getNumOfComment(ID feedsId) {
        Object[] c = Application.createQueryNoFilter(
                "select count(commentId) from FeedsComment where feedsId = ?")
                .setParameter(1, feedsId)
                .unique();
        return c == null ? 0 : ObjectUtils.toInt(c[0]);
    }

    /**
     * 点赞数
     * TODO 缓存
     *
     * @param feedsOrComment
     * @return
     */
    public static int getNumOfLike(ID feedsOrComment) {
        Object[] c = Application.createQueryNoFilter(
                "select count(likeId) from FeedsLike where source = ?")
                .setParameter(1, feedsOrComment)
                .unique();
        return c == null ? 0 : ObjectUtils.toInt(c[0]);
    }

    /**
     * 指定用户是否点赞
     *
     * @param feedsOrComment
     * @param user 指定用户
     * @return
     */
    public static boolean isMyLike(ID feedsOrComment, ID user) {
        Object[] c = Application.createQueryNoFilter(
                "select likeId from FeedsLike where source = ? and createdBy = ?")
                .setParameter(1, feedsOrComment)
                .setParameter(2, user)
                .unique();
        return c != null;
    }

    /**
     * 获取内容中的 @USERID
     *
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
     * @return Returns Map<@NAME, @ID>
     */
    public static Map<String, ID> findMentionsMap(String content) {
        Map<String, ID> found = new HashMap<>();
        for (String ats : content.split("@")) {
            if (StringUtils.isBlank(ats)) {
                continue;
            }
            String[] atss = ats.split(" ");

            String fullName = atss[0];
            // 全名
            ID user = UserHelper.findUserByFullName(fullName);
            // 用户名
            if (user == null && Application.getUserStore().existsName(fullName)) {
                user = Application.getUserStore().getUser(fullName).getId();
            }

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
     * 用户对指定动态是否可读
     *
     * @param feedsOrComment
     * @param user
     * @return
     */
    public static boolean checkReadable(ID feedsOrComment, ID user) {
        String sql = "select scope,createdBy from Feeds where feedsId = ?";
        if (feedsOrComment.getEntityCode() == EntityHelper.FeedsComment) {
            sql = "select feedsId.scope,feedsId.createdBy from FeedsComment where feedsId = ?";
        }

        Object[] o = Application.createQueryNoFilter(sql).setParameter(1, feedsOrComment).unique();
        if (o == null) {
            return false;
        }
        if (o[1].equals(user) || o[0].equals(FeedsScope.ALL.name())) {
            return true;  // 自己 & 公开
        }

        // 团队
        if (ID.isId(o[0])) {
            Team team = Application.getUserStore().getTeam(ID.valueOf((String) o[0]));
            return team.isMember(user);
        }
        return false;
    }
}
