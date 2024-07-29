/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    private static final List<HttpSession> ONLINE_USERS = new ArrayList<>();
    private static final Map<String, Object[]> ONLINE_USERS_H5 = new ConcurrentHashMap<>();

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
        ONLINE_SESSIONS.remove(s);
        synchronized (ONLINE_USERS) {
            ONLINE_USERS.remove(s);
        }
    }

    /**
     * 所有会话
     *
     * @return
     */
    public Set<HttpSession> getAllSession() {
        Set<HttpSession> alls = new HashSet<>();
        alls.addAll(ONLINE_SESSIONS);
        alls.addAll(ONLINE_USERS);
        return Collections.unmodifiableSet(alls);
    }

    /**
     * 用户会话
     *
     * @param user
     * @return
     */
    public HttpSession getSession(ID user) {
        for (HttpSession s : ONLINE_SESSIONS) {
            Object inSession = s.getAttribute(WebUtils.CURRENT_USER);
            if (user.equals(inSession)) return s;
        }
        return null;
    }

    /**
     * @param request
     * @param h5NoKill
     */
    public void storeLoginSuccessed(HttpServletRequest request, boolean h5NoKill) {
        HttpSession s = request.getSession();
        Object loginUser = s.getAttribute(WebUtils.CURRENT_USER);
        Assert.notNull(loginUser, "No login user found in session!");

        if (!RebuildConfiguration.getBool(ConfigurationItem.MultipleSessions)) {
            HttpSession previous = h5NoKill ? null : getSession((ID) loginUser);
            if (previous != null) {
                log.warn("Kill previous session : {} ({})", previous.getId(), loginUser);

                try {
                    previous.invalidate();
                } catch (Throwable ignored) {
                }
            }
        }

        synchronized (ONLINE_USERS) {
            ONLINE_USERS.add(s);
        }
    }

    /**
     * @param request
     */
    public void storeLastActive(HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        if (requestUri.contains("/filex/access/")) {
            return;
        }

        HttpSession s = request.getSession();
        s.setAttribute(SK_LASTACTIVE,
                new Object[]{System.currentTimeMillis(), requestUri, ServletUtils.getRemoteAddr(request)});
    }

    /**
     * @param sessionOrUser
     * @return
     */
    public boolean killSession(Object sessionOrUser) {
        HttpSession found = null;
        // User
        if (sessionOrUser instanceof ID) {
            found = getSession((ID) sessionOrUser);
        }
        // SessionID or AuthToken
        else {
            for (HttpSession s : getAllSession()) {
                if (s.getId().equals(sessionOrUser)) {
                    found = s;
                    break;
                }
            }

            // H5 AuthToken
            if (found == null) {
                for (Object[] s : getAllH5Session(false)) {
                    if (sessionOrUser.equals(s[3])) {
                        AuthTokenManager.verifyToken((String) s[3], true, false);
                        getAllH5Session(true);
                        return true;
                    }
                }
            }
        }

        try {
            if (found != null) {
                found.invalidate();
                log.warn("Kill session with {}", sessionOrUser);
            }
        } catch (Exception ignored) {}
        return found != null;
    }

    // for Mobile/H5

    /**
     * @param authToken
     * @param user
     * @param activeUrl
     * @param request
     */
    public void storeH5LastActive(String authToken, ID user, String activeUrl, HttpServletRequest request) {
        ONLINE_USERS_H5.put(authToken,
                new Object[]{System.currentTimeMillis(), activeUrl, ServletUtils.getRemoteAddr(request), authToken, user});
    }

    /**
     * @param user
     * @return
     */
    public String getH5Session(ID user) {
        for (Object[] s : ONLINE_USERS_H5.values()) {
            if (user.equals(s[4])) return (String) s[3];
        }
        return null;
    }

    /**
     * @param clearInvalid
     * @return
     */
    public Collection<Object[]> getAllH5Session(boolean clearInvalid) {
        if (clearInvalid) {
            for (String token : ONLINE_USERS_H5.keySet()) {
                ID valid = AuthTokenManager.verifyToken(token);
                if (valid == null) ONLINE_USERS_H5.remove(token);
            }
            log.info("Clean H5 sessions. Current : {}", ONLINE_USERS_H5.size());
        }

        return ONLINE_USERS_H5.values();
    }
}
