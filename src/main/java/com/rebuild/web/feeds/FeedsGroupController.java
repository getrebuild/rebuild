/*
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
@RestController
@RequestMapping("/feeds/group/")
public class FeedsGroupController extends BaseController {

    @GetMapping("group-list")
    public JSON groupList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");

        JSONArray ret = new JSONArray();

        Set<Member> stars = getStars(user, EntityHelper.Team);
        Set<Serializable> starsId = new HashSet<>();
        for (Member t : UserHelper.sortMembers(stars.toArray(new Member[0]))) {
            if (StringUtils.isEmpty(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                ret.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name", "star"},
                        new Object[]{t.getIdentity(), t.getName(), true}));
                starsId.add(t.getIdentity());
            }
        }

        Set<Team> teams = Application.getUserStore().getUser(user).getOwningTeams();
        for (Member t : UserHelper.sortMembers(teams.toArray(new Member[0]))) {
            if (StringUtils.isEmpty(query)
                    || StringUtils.containsIgnoreCase(t.getName(), query)) {
                if (starsId.contains(t.getIdentity())) continue;

                ret.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name"},
                        new Object[]{t.getIdentity(), t.getName()}));
                if (ret.size() >= 20) break;
            }
        }

        return ret;
    }

    @GetMapping("user-list")
    public JSON userList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String query = getParameter(request, "q");

        JSONArray ret = new JSONArray();

        Set<Member> stars = getStars(user, EntityHelper.User);
        Set<ID> starsId = new HashSet<>();
        for (Member m : UserHelper.sortMembers(stars.toArray(new Member[0]))) {
            User u = (User) m;
            if (StringUtils.isEmpty(query)
                    || CommonsUtils.containsIgnoreCase(new String[] { u.getName(), u.getFullName(), u.getEmail() }, query)) {
                ret.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name", "star"},
                        new Object[]{u.getId(), u.getFullName(), true}));
                starsId.add(u.getId());
            }
        }

        for (User u : UserHelper.sortUsers()) {
            if (StringUtils.isBlank(query)
                    || CommonsUtils.containsIgnoreCase(new String[] { u.getName(), u.getFullName(), u.getEmail() }, query)) {
                if (starsId.contains(u.getId())) continue;

                ret.add(JSONUtils.toJSONObject(
                        new String[]{"id", "name"},
                        new Object[]{u.getId(), u.getFullName()}));
                if (ret.size() >= 20) break;
            }
        }

        return ret;
    }

    private static final String FEED_STARS = "FeedUserStars.";

    @PostMapping("star-toggle")
    public RespBody stars(HttpServletRequest request) {
        ID starUser = getIdParameterNotNull(request, "user");

        String key = FEED_STARS + getRequestUser(request);
        String feedStars = KVStorage.getCustomValue(key);

        if (feedStars != null && feedStars.contains(starUser.toLiteral())) {
            feedStars = feedStars
                    .replace("," + starUser, "")
                    .replace(starUser.toLiteral(), "");
        } else {
            if (feedStars == null) feedStars = "";
            feedStars += "," + starUser;
        }
        KVStorage.setCustomValue(key, feedStars);

        // FIXME 可能导致值越来越大，因为可能存在删除的 user

        return RespBody.ok();
    }

    private Set<Member> getStars(ID user, int type) {
        String key = FEED_STARS + user;
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
