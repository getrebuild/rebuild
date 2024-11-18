/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.audit;

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
import com.rebuild.web.user.signup.LoginChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

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
        JSON config = DataListManager.instance.getListFields("LoginLog", user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));
        return mv;
    }

    @GetMapping("/admin/audit/online-users")
    public JSON getOnlineUsers(HttpServletRequest request) {
        final String currentSid = request.getSession().getId();

        JSONArray online = new JSONArray();
        for (Map.Entry<String, OnlineSessionStore.ActiveInfo> e
                : Application.getSessionStore().getAllActiveInfos().entrySet()) {
            String[] keySplit = e.getKey().split(";");
            OnlineSessionStore.ActiveInfo info = e.getValue();

            ID user = ID.valueOf(keySplit[0]);
            String usName = UserHelper.getName(user);
            if (currentSid.equals(info.getSessionId())) usName += " [CURRENT]";
            String chName = LoginChannel.valueOf(keySplit[1]).getName();

            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"user", "fullName", "activeTime", "activeUrl", "activeIp", "channel", "sid", "token"},
                    new Object[]{user, usName, info.getActiveTime(), info.getActiveUri(), info.getActiveIp(), chName, info.getSessionId(), info.getAuthToken()});
            online.add(item);
        }

        online.sort((o1, o2) -> {
            long d1 = ((JSONObject) o1).getLong("activeTime");
            long d2 = ((JSONObject) o2).getLong("activeTime");
            return Long.compare(d2, d1);
        });
        for (Object o : online) {
            long activeTime = ((JSONObject) o).getLong("activeTime");
            ((JSONObject) o).put("activeTime", I18nUtils.formatDate(new Date(activeTime)));
        }
        return online;
    }

    @RequestMapping("/admin/audit/kill-session")
    public RespBody killSession(HttpServletRequest request) {
        String sessionIdOrToken = getParameterNotNull(request, "user");
        Application.getSessionStore().killSession(sessionIdOrToken);
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
