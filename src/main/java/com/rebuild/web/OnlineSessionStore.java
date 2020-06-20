/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.CurrentCaller;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.user.signin.LoginControll;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NamedThreadLocal;
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

	private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Current session user");

	/**
	 * 最近访问 [时间, 路径]
	 * @see #storeLastActive(HttpServletRequest)
	 */
	public static final String SK_LASTACTIVE = WebUtils.KEY_PREFIX + "Session-LastActive";
	
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
	 * 用户会话
	 *
	 * @param user
	 * @return
	 */
	public HttpSession getSession(ID user) {
		return ONLINE_USERS.get(user);
	}
	
	/**
	 * @param request
	 */
	public void storeLastActive(HttpServletRequest request) {
		HttpSession s = request.getSession();
		s.setAttribute(SK_LASTACTIVE, new Object[] { System.currentTimeMillis(), request.getRequestURI() } );
	}

	/**
	 * @param request
	 */
	public void storeLoginSuccessed(HttpServletRequest request) {
		HttpSession s = request.getSession();
		Object loginUser = s.getAttribute(WebUtils.CURRENT_USER);
		Assert.notNull(loginUser, "No login user found in session!");

		if (!SysConfiguration.getBool(ConfigurableItem.MultipleSessions)) {
			HttpSession previous = getSession((ID) loginUser);
			if (previous != null) {
				LOG.warn("Kill previous session : " + loginUser + " < " + previous.getId());
				try {
					previous.invalidate();
				} catch (Exception ignored) {
				}
			}
		}

		ONLINE_SESSIONS.remove(s);
		ONLINE_USERS.put((ID) loginUser, s);
	}

	/**
	 * @param locale
	 */
	public void setLocale(String locale) {
		LOCALE.set(locale);
	}

	/**
	 * @return Returns default if unset
	 * @see Languages
	 */
	public String getLocale() {
		return StringUtils.defaultIfEmpty(LOCALE.get(), SysConfiguration.get(ConfigurableItem.DefaultLanguage));
	}

	/**
	 * @param caller
	 * @param locale
	 * @see #set(ID)
	 */
	public void set(ID caller, String locale) {
		super.set(caller);
		this.setLocale(locale);
	}

	@Override
	public void clean() {
		super.clean();
		LOCALE.remove();
	}
}
