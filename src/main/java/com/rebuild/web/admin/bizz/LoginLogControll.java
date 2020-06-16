/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.LocationUtils;
import com.rebuild.web.BaseEntityControll;
import com.rebuild.web.OnlineSessionStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;

/**
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/02/16
 */
@Controller
public class LoginLogControll extends BaseEntityControll {

	@RequestMapping("/admin/bizuser/login-logs")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ModelAndView mv = createModelAndView("/admin/bizuser/login-logs.jsp", "LoginLog", user);
		JSON config = DataListManager.instance.getFieldsLayout("LoginLog", user);
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		return mv;
	}

	@RequestMapping("/commons/ip-location")
	public void getIpLocation(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String ip = getParameterNotNull(request, "ip");
		try {
			JSON location = LocationUtils.getLocation(ip);
			writeSuccess(response, location);
		} catch (Exception ex) {
			writeFailure(response);
		}
	}

	@RequestMapping("/admin/bizuser/online-users")
	public void getOnlineUsers(HttpServletResponse response) throws IOException {
		JSONArray users = new JSONArray();
		for (HttpSession s : Application.getSessionStore().getAllSession()) {
			ID user = (ID) s.getAttribute(WebUtils.CURRENT_USER);
			if (user == null) continue;

			Object[] active = (Object[]) s.getAttribute(OnlineSessionStore.SK_LASTACTIVE);
			if (active == null) {
				active = new Object[] { "", "/dashboard/home" };
			} else {
				active = active.clone();
				active[0] = Moment.moment(new Date((Long) active[0])).fromNow();
			}

			JSONObject item = JSONUtils.toJSONObject(
					new String[] { "user", "fullName", "activeTime", "activeUrl" },
					new Object[] { user, UserHelper.getName(user), active[0], active[1] } );
			users.add(item);
		}
		writeSuccess(response, users);
	}

	@RequestMapping("/admin/bizuser/kill-session")
	public void killSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getIdParameterNotNull(request, "user");
		HttpSession s = Application.getSessionStore().getSession(user);
		if (s != null) {
			LOG.warn("Kill session via admin : " + user + " < " + s.getId());
			try {
				s.invalidate();
			} catch (Exception ignored) {
			}
		}
		writeSuccess(response);
	}
}
