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
import java.util.Collection;
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
    // v3.8
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

        for (Map.Entry<ID, HttpSession> e : ONLINE_USERS.entrySet()) {
            if (s.equals(e.getValue())) {
                ONLINE_USERS.remove(e.getKey());
                break;
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
        final String requestUri = request.getRequestURI();
        if (requestUri.contains("/filex/access/")) {
            return;
        }

        HttpSession s = request.getSession();
        s.setAttribute(SK_LASTACTIVE,
                new Object[]{System.currentTimeMillis(), requestUri, ServletUtils.getRemoteAddr(request)});
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

        ONLINE_USERS.put((ID) loginUser, s);
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
        // SessionID
        else {
            for (HttpSession s : getAllSession()) {
                if (s.getId().equals(sessionOrUser)) {
                    found = s;
                    break;
                }
            }

            // for H5
            if (found == null) {
                String token = sessionOrUser.toString();
                ID d = AuthTokenManager.verifyToken(token, true, false);
                if (d != null) {
                    ONLINE_USERS_H5.remove(token);
                    return true;
                }
                return false;
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

    // for H5/Mobile

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
            log.info("Clean H5 sessions. Current valid : {}", ONLINE_USERS_H5.size());
        }

        return ONLINE_USERS_H5.values();
    }
}
