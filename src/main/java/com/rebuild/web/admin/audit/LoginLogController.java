/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.audit;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.LocationUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.OnlineSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/02/16
 */
@Slf4j
@RestController
public class LoginLogController extends EntityController {

    @GetMapping("/admin/audit/login-logs")
    public ModelAndView pageList(HttpServletRequest request) {
        ID user = getRequestUser(request);
        ModelAndView mv = createModelAndView("/admin/audit/login-logs", "LoginLog", user);
        JSON config = DataListManager.instance.getFieldsLayout("LoginLog", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @GetMapping("/admin/audit/online-users")
    public JSON getOnlineUsers() {
        JSONArray users = new JSONArray();
        for (HttpSession s : Application.getSessionStore().getAllSession()) {
            ID user = (ID) s.getAttribute(WebUtils.CURRENT_USER);
            if (user == null) continue;

            Object[] active = (Object[]) s.getAttribute(OnlineSessionStore.SK_LASTACTIVE);
            if (active == null) {
                active = new Object[]{ null, "/dashboard/home", null };
            } else {
                active = active.clone();
                active[0] = I18nUtils.formatDate(new Date((Long) active[0]));
            }

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "user", "fullName", "activeTime", "activeUrl", "activeIp" },
                    new Object[] { user, UserHelper.getName(user), active[0], active[1], active[2] });
            users.add(item);
        }
        return users;
    }

    @RequestMapping("/admin/audit/kill-session")
    public RespBody killSession(HttpServletRequest request) {
        final ID user = getIdParameterNotNull(request, "user");

        HttpSession s = Application.getSessionStore().getSession(user);
        if (s != null) {
            log.warn("Kill session via admin : " + user + " < " + s.getId());
            try {
                s.invalidate();
            } catch (Exception ignored) {
            }
        }
        return RespBody.ok();
    }

    @GetMapping("/commons/ip-location")
    public RespBody getIpLocation(HttpServletRequest request) {
        String ip = getParameterNotNull(request, "ip");
        try {
            JSON location = LocationUtils.getLocation(ip);
            return RespBody.ok(location);

        } catch (Exception ex) {
            return RespBody.error();
        }
    }
}
