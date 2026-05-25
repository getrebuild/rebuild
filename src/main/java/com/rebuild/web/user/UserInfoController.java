/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 2021/8/23
 */
@RestController
public class UserInfoController {

    @GetMapping("/account/user-info")
    public JSON userInfo(@IdParam ID id) {
        User u = Application.getUserStore().getUser(id);
        Department dept = u.getOwningDept();
        return JSONUtils.toJSONObject(
                new String[]{"name", "dept", "email", "phone", "isActive"},
                new Object[]{u.getFullName(), dept == null ? null : dept.getName(), u.getEmail(), u.getWorkphone(), u.isActive() });
    }

    @RequestMapping("/account/check-user-status")
    public RespBody checkUserStatus(@IdParam ID uid) {
        if (!Application.getUserStore().existsUser(uid)) return RespBody.error();

        final User checkedUser = Application.getUserStore().getUser(uid);

        Map<String, Object> ret = new HashMap<>();
        ret.put("active", checkedUser.isActive());
        ret.put("system", uid.equals(UserService.ADMIN_USER) || uid.equals(UserService.SYSTEM_USER));
        ret.put("disabled", checkedUser.isDisabled());

        if (checkedUser.getOwningRole() != null) {
            ret.put("role", checkedUser.getOwningRole().getIdentity());
            ret.put("roleDisabled", checkedUser.getOwningRole().isDisabled());

            // 附加角色
            ret.put("roleAppends", UserHelper.getRoleAppends(uid));
        }

        Set<Object> teams = new HashSet<>();
        for (Team team : checkedUser.getOwningTeams()) {
            teams.add(team.getIdentity());
        }
        // v4.2 加入团队
        ret.put("joinTeams", teams);

        if (checkedUser.getOwningDept() != null) {
            ret.put("dept", checkedUser.getOwningDept().getIdentity());
            ret.put("deptDisabled", checkedUser.getOwningDept().isDisabled());
        }

        Object[] lastLogin = Application.createQueryNoFilter(
                        "select loginTime,ipAddr from LoginLog where user = ? order by loginTime desc")
                .setParameter(1, uid)
                .unique();
        if (lastLogin != null) {
            ret.put("lastLogin",
                    new Object[] { I18nUtils.formatDate((Date) lastLogin[0]), lastLogin[1] });
        }

        return RespBody.ok(ret);
    }
}
