/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.commons.ObjectUtils;
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
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

        JSON config = DataListManager.instance.getFieldsLayout("User", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        mv.getModel().put("serviceMail", SMSender.availableMail());
        return mv;
    }

    @RequestMapping("check-user-status")
    public RespBody checkUserStatus(@IdParam ID userId) {
        if (!Application.getUserStore().existsUser(userId)) {
            return RespBody.error();
        }

        User checkedUser = Application.getUserStore().getUser(userId);

        Map<String, Object> ret = new HashMap<>();
        ret.put("active", checkedUser.isActive());
        ret.put("system", "system".equals(checkedUser.getName()) || "admin".equals(checkedUser.getName()));
        ret.put("disabled", checkedUser.isDisabled());

        if (checkedUser.getOwningRole() != null) {
            ret.put("role", checkedUser.getOwningRole().getIdentity());
            ret.put("roleDisabled", checkedUser.getOwningRole().isDisabled());

            // 附加角色
            ret.put("roleAppends", UserHelper.getRoleAppends(userId));
        }

        if (checkedUser.getOwningDept() != null) {
            ret.put("dept", checkedUser.getOwningDept().getIdentity());
            ret.put("deptDisabled", checkedUser.getOwningDept().isDisabled());
        }

        return RespBody.ok(ret);
    }

    @PostMapping("enable-user")
    public RespBody enableUser(HttpServletRequest request) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        ID userId = ID.valueOf(data.getString("user"));
        User enUser = Application.getUserStore().getUser(userId);

        // 当前是从未激活状态
        final boolean beforeUnEnabled = enUser.isDisabled()
                && (enUser.getOwningDept() == null || enUser.getOwningRole() == null);

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

        // 是否需要发送激活通知
        enUser = Application.getUserStore().getUser(userId);
        if (beforeUnEnabled && enUser.isActive() && SMSender.availableMail() && enUser.getEmail() != null) {
            Object did = Application.createQuery(
                    "select logId from LoginLog where user = ?")
                    .setParameter(1, enUser.getId())
                    .unique();

            if (did == null) {
                String homeUrl = RebuildConfiguration.getHomeUrl();
                String subject = getLang(request, "YourAccountActive");
                String content = String.format(getLang(request, "NewUserAccountActive"), enUser.getFullName(), homeUrl, homeUrl);

                SMSender.sendMailAsync(enUser.getEmail(), subject, content);
            }
        }

        // 禁用后马上使之登录失效
        if (!enUser.isActive()) {
            HttpSession s = Application.getSessionStore().getSession(enUser.getId());
            if (s != null) {
                log.warn("Force destroy user session : " + enUser.getId());
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
            hasMember += UserHelper.getRoleMembers(bizzId).size();

        } else if (bizzId.getEntityCode() == EntityHelper.User) {
            // NOTE 仅检查是否登陆过。严谨些还应该检查是否有其他业务数据
            Object[] hasLogin = Application.createQueryNoFilter(
                    "select count(logId) from LoginLog where user = ?")
                    .setParameter(1, bizzId)
                    .unique();
            hasMember = ObjectUtils.toInt(hasLogin[0]);
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

        return RespBody.ok();
    }
}