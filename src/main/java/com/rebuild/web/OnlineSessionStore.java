/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.CurrentCaller;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.user.signin.LoginControll;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 在线会话/用户
 * 
 * @author devezhao
 * @since 09/27/2018
 */
public class OnlineSessionStore extends CurrentCaller implements HttpSessionListener {

	private static final Log LOG = LogFactory.getLog(OnlineSessionStore.class);
	
	private static final Set<HttpSession> ONLINE_SESSIONS = new CopyOnWriteArraySet<>();
	private static final Map<ID, HttpSession> ONLINE_USERS = new ConcurrentHashMap<>();
	
	@Override
	public void sessionCreated(HttpSessionEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.info("Created session - " + event);
		}
		ONLINE_SESSIONS.add(event.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.info("Destroyed session - " + event);
		}
		
		HttpSession s = event.getSession();
		if (ONLINE_SESSIONS.contains(s)) {
			ONLINE_SESSIONS.remove(s);
		} else {
			for (Map.Entry<ID, HttpSession> e : ONLINE_USERS.entrySet()) {
				if (s.equals(e.getValue())) {
					ONLINE_USERS.remove(e.getKey());
					break;
				}
			}
		}
		
		// Logout time
		ID loginId = (ID) s.getAttribute(LoginControll.SK_LOGINID);
		if (loginId != null) {
			Record logout = EntityHelper.forUpdate(loginId, UserService.SYSTEM_USER);
			logout.setDate("logoutTime", CalendarUtils.now());
			Application.getCommonService().update(logout);
		}
	}
	
	// --
	
	/**
	 * 最近访问时间
	 */
	public static final String SK_LASTACTIVE = WebUtils.KEY_PREFIX + "Session-LastActive";
	/**
	 * 最近访问路径
	 */
	public static final String SK_LASTACCESS = WebUtils.KEY_PREFIX + "Session-LastAccess";
	
	/**
	 * 所有会话
	 * 
	 * @return
	 */
	public Set<HttpSession> getAllSession() {
		Set<HttpSession> all = new HashSet<>();
		all.addAll(ONLINE_SESSIONS);
		all.addAll(ONLINE_USERS.values());
		return all;
	}
	
	/**
	 * @param request
	 */
	public void storeLastActive(HttpServletRequest request) {
		HttpSession s = request.getSession(true);
		s.setAttribute(SK_LASTACTIVE, CalendarUtils.now());
	}
	
	/**
	 * @param request
	 */
	public void storeLoginSuccessed(HttpServletRequest request) {
		HttpSession s = request.getSession();
		Object loginUser = s.getAttribute(WebUtils.CURRENT_USER);
		Assert.notNull(loginUser, "No login user found");

		ONLINE_SESSIONS.remove(s);
		ONLINE_USERS.put((ID) loginUser, s);
	}
}
