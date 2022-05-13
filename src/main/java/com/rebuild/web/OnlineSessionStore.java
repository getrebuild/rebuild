/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Collections;
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
@Slf4j
@Component
public class OnlineSessionStore implements HttpSessionListener {

    private static final Set<HttpSession> ONLINE_SESSIONS = new CopyOnWriteArraySet<>();
    private static final Map<ID, HttpSession> ONLINE_USERS = new ConcurrentHashMap<>();

    /**
     * 最近访问 [时间, 路径]
     *
     * @see #storeLastActive(HttpServletRequest)
     */
    public static final String SK_LASTACTIVE = WebUtils.KEY_PREFIX + "Session-LastActive";

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        if (log.isDebugEnabled()) log.info("Created session : {}", event.getSession().getId());

        ONLINE_SESSIONS.add(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        if (log.isDebugEnabled()) log.info("Destroyed session : {}", event.getSession().getId());

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
        return Collections.unmodifiableSet(all);
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
        s.setAttribute(SK_LASTACTIVE,
                new Object[]{System.currentTimeMillis(), request.getRequestURI(), ServletUtils.getRemoteAddr(request)});
    }

    /**
     * @param request
     */
    public void storeLoginSuccessed(HttpServletRequest request) {
        HttpSession s = request.getSession();
        Object loginUser = s.getAttribute(WebUtils.CURRENT_USER);
        Assert.notNull(loginUser, "No login user found in session!");

        if (!RebuildConfiguration.getBool(ConfigurationItem.MultipleSessions)) {
            HttpSession previous = getSession((ID) loginUser);
            if (previous != null) {
                log.warn("Kill previous session : {} < {}", loginUser, previous.getId());

                try {
                    previous.invalidate();
                } catch (Throwable ignored) {
                }
            }
        }

        ONLINE_SESSIONS.remove(s);
        ONLINE_USERS.put((ID) loginUser, s);
    }
}
