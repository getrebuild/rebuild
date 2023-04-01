/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.KVStorage;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.UsersGetting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 群组 & 团队
 *
 * @author devezhao
 * @see Team
 * @since 2019/11/8
 */
@SuppressWarnings("unchecked")
@RestController
@RequestMapping("/feeds/group/")
public class FeedsGroupController extends BaseController {

    @GetMapping("group-list")
    public JSON groupList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");

        JSONArray res = new JSONArray();

        Set<Member> stars = getStars(user, EntityHelper.Team);
        stars = (Set<Member>) filterMembers32(stars, user);

        Set<Serializable> starsId = new HashSet<>();
        for (Member t : UserHelper.sortMembers(stars.toArray(new Member[0]))) {
            if (StringUtils.isEmpty(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                res.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name", "star"},
                        new Object[]{t.getIdentity(), t.getName(), true}));
                starsId.add(t.getIdentity());
            }
        }

        Team[] teams = Application.getUserStore().getAllTeams();
        for (Member t : UserHelper.sortMembers((Member[]) filterMembers32(teams, user))) {
            if (StringUtils.isEmpty(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                if (starsId.contains(t.getIdentity())) continue;

                res.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name"},
                        new Object[]{t.getIdentity(), t.getName()}));
                if (res.size() >= 20) break;
            }
        }

        return res;
    }

    @GetMapping("user-list")
    public JSON userList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");

        JSONArray res = new JSONArray();

        Set<Member> stars = getStars(user, EntityHelper.User);
        stars = (Set<Member>) filterMembers32(stars, user);

        Set<ID> starsId = new HashSet<>();
        for (Member m : UserHelper.sortMembers(stars.toArray(new Member[0]))) {
            User u = (User) m;
            if (StringUtils.isEmpty(query)
                    || CommonsUtils.containsIgnoreCase(new String[] { u.getName(), u.getFullName(), u.getEmail() }, query)) {
                res.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name", "star"},
                        new Object[]{u.getId(), u.getFullName(), true}));
                starsId.add(u.getId());
            }
        }

        User[] users = UserHelper.sortUsers();
        Member[] users2 = (Member[]) filterMembers32(users, user);
        for (Member m : users2) {
            User u = (User) m;
            if (StringUtils.isBlank(query)
                    || CommonsUtils.containsIgnoreCase(new String[] { u.getName(), u.getFullName(), u.getEmail() }, query)) {
                if (starsId.contains(u.getId())) continue;

                res.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name"},
                        new Object[]{u.getId(), u.getFullName()}));
                if (res.size() >= 20) break;
            }
        }

        return res;
    }

    private Object filterMembers32(Object members, ID currentUser) {
        if (members instanceof Member[]) {
            return UsersGetting.filterMembers32((Member[]) members, currentUser);
        }

        // Set
        Member[] members2 = ((Set<Member>) members).toArray(new Member[0]);
        members2 = UsersGetting.filterMembers32(members2, currentUser);

        Set<Member> set2 = new HashSet<>();
        CollectionUtils.addAll(set2, members2);
        return set2;
    }

    private static final String FEED_STARS = "FeedUserStars.";

    @PostMapping("star-toggle")
    public RespBody stars(HttpServletRequest request) {
        ID starUser = getIdParameterNotNull(request, "user");

        final String key = FEED_STARS + getRequestUser(request);
        String feedStars = KVStorage.getCustomValue(key);

        if (feedStars != null && feedStars.contains(starUser.toLiteral())) {
            feedStars = feedStars
                    .replace("," + starUser, "")
                    .replace(starUser.toLiteral(), "");
        } else {
            if (feedStars == null) feedStars = "";
            feedStars += "," + starUser;
        }

        Set<String> clearStars = new HashSet<>();
        for (String id : feedStars.split(",")) {
            if (!ID.isId(id)) continue;
            if (Application.getUserStore().existsUser(ID.valueOf(id))) clearStars.add(id);
        }

        KVStorage.setCustomValue(key, StringUtils.join(clearStars, ","));

        return RespBody.ok();
    }

    private Set<Member> getStars(ID user, int type) {
        final String key = FEED_STARS + user;
        String feedStars = StringUtils.defaultString(KVStorage.getCustomValue(key), "");

        Set<Member> set = new HashSet<>();
        for (String s : StringUtils.split(feedStars, ",")) {
            if (!ID.isId(s)) continue;

            ID id = ID.valueOf(s);
            if (id.getEntityCode() == type && UserHelper.isActive(id)) {
                if (type == EntityHelper.User) set.add(Application.getUserStore().getUser(id));
                else if (type == EntityHelper.Team) set.add(Application.getUserStore().getTeam(id));
            }
        }

        return set;
    }
}
