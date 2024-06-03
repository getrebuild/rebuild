/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author devezhao
 * @since 10/08/2018
 */
@Slf4j
@RestController
@RequestMapping("/admin/bizuser/")
public class UserController extends EntityController {

    @GetMapping("users")
    public ModelAndView pageList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/bizuser/user-list", "User", user);

        JSON config = DataListManager.instance.getListFields("User", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        mv.getModel().put("serviceMail", SMSender.availableMail());
        return mv;
    }

    @RequestMapping("check-user-status")
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

    @PostMapping("enable-user")
    public RespBody enableUser(@RequestBody JSONObject data) {
        final ID userId = ID.valueOf(data.getString("user"));
        User enUser = Application.getUserStore().getUser(userId);

        ID deptNew = null;
        ID roleNew = null;
        ID[] roleAppends = null;

        if (data.containsKey("dept")) {
            deptNew = ID.valueOf(data.getString("dept"));
            if (enUser.getOwningDept() != null && enUser.getOwningDept().getIdentity().equals(deptNew)) {
                deptNew = null;
            }
        }

        if (data.containsKey("role")) {
            roleNew = ID.valueOf(data.getString("role"));
            if (enUser.getOwningRole() != null && enUser.getOwningRole().getIdentity().equals(roleNew)) {
                roleNew = null;
            }
        }

        if (data.containsKey("roleAppends")) {
            String appends = data.getString("roleAppends");
            Set<ID> set = new HashSet<>();
            for (String s : appends.split(",")) {
                if (ID.isId(s)) set.add(ID.valueOf(s));
            }

            if (roleNew != null) {
                set.remove(deptNew);
            } else if (enUser.getOwningRole() != null) {
                // noinspection SuspiciousMethodCalls
                set.remove(enUser.getOwningRole().getIdentity());
            }

            if (!set.isEmpty()) {
                roleAppends = set.toArray(new ID[0]);
            }
        }

        Boolean enableNew = null;
        if (data.containsKey("enable")) {
            enableNew = data.getBoolean("enable");
        }

        Application.getBean(UserService.class)
                .updateEnableUser(userId, deptNew, roleNew, roleAppends, enableNew);

        // 禁用后马上销毁会话
        enUser = Application.getUserStore().getUser(enUser.getId());
        if (!enUser.isActive()) {
            HttpSession s = Application.getSessionStore().getSession(enUser.getId());
            if (s != null) {
                log.warn("FORCE DESTROY USER SESSION : {} < {}", enUser.getId(), s.getId());
                s.invalidate();
            }
        }

        return RespBody.ok();
    }

    @RequestMapping("delete-checks")
    public JSON deleteChecks(@IdParam ID bizzId) {
        int hasMember = 0;
        int hasChild = 0;

        if (bizzId.getEntityCode() == EntityHelper.Department) {
            Department dept = Application.getUserStore().getDepartment(bizzId);
            hasMember = dept.getMembers().size();
            hasChild = dept.getChildren().size();

        } else if (bizzId.getEntityCode() == EntityHelper.Role) {
            hasMember = UserHelper.getMembers(bizzId).length;
            hasMember += UserHelper.getMembersOfRole(bizzId).size();

        } else if (bizzId.getEntityCode() == EntityHelper.User) {
            hasMember = UserService.checkHasUsed(bizzId) ? 1 : 0;
        }

        return JSONUtils.toJSONObject(
                new String[] { "hasMember", "hasChild"},
                new Object[] { hasMember, hasChild });
    }

    @PostMapping("user-delete")
    public RespBody userDelete(@IdParam ID userId) {
        Application.getBean(UserService.class).delete(userId);
        return RespBody.ok();
    }

    @PostMapping("user-resetpwd")
    public RespBody userResetpwd(@IdParam ID userId, HttpServletRequest request) {
        String newp = getParameterNotNull(request, "newp");

        Record record = EntityHelper.forUpdate(userId, userId);
        record.setString("password", newp);
        Application.getBean(UserService.class).update(record);

        if (getBoolParameter(request, "email")) {
            String email = Application.getUserStore().getUser(userId).getEmail();
            if (email != null && SMSender.availableMail()) {
                String subject = Language.L("密码已被管理员重置");
                String content = Language.L("你的密码已被管理员重置，请使用新密码登录。[][] 新密码：**%s**", newp);
                SMSender.sendMailAsync(email, subject, content);
            }
        }

        return RespBody.ok();
    }

    @PostMapping("bizz-flag")
    public JSON bizzFlags(HttpServletRequest request) {
        String post = ServletUtils.getRequestString(request);
        String[] ids = post == null ? new String[0] : post.split(",");

        JSONObject resMap = new JSONObject();

        for (String id : ids) {
            if (!ID.isId(id)) continue;

            Object[] o = Application.getQueryFactory().unique(ID.valueOf(id), "externalId");
            if (o != null && o[0] != null) resMap.put(id, o[0]);
        }
        return resMap;
    }
}