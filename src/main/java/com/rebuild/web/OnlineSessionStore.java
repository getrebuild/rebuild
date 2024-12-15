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
import com.rebuild.utils.AppUtils;
import com.rebuild.web.user.signup.LoginChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Collections;
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
    private static final Map<String, HttpSession> ONLINE_USERS = new ConcurrentHashMap<>();
    private static final Map<String, ActiveInfo> ACTIVE_INFOS39 = new ConcurrentHashMap<>();

    private static final String SK_LOGINCHANNEL = WebUtils.KEY_PREFIX + "Session-LoginChannel";

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

        for (Map.Entry<String, HttpSession> e : ONLINE_USERS.entrySet()) {
            if (s.equals(e.getValue())) {
                ONLINE_USERS.remove(e.getKey());
                ACTIVE_INFOS39.remove(e.getKey());
                break;
            }
        }
    }

    /**
     * 用户会话
     *
     * @param user
     * @param loginChannel
     * @return
     */
    public HttpSession getSession(ID user, LoginChannel loginChannel) {
        String key = user + ";" + loginChannel.name();
        return ONLINE_USERS.get(key);
    }

    /**
     * @return
     */
    public static Set<HttpSession> getAllSessions() {
        return Collections.unmodifiableSet(ONLINE_SESSIONS);
    }

    /**
     * @return
     */
    public Map<String, ActiveInfo> getAllActiveInfos() {
        return Collections.unmodifiableMap(ACTIVE_INFOS39);
    }

    /**
     * 最近活跃
     *
     * @param user
     * @param request
     * @param requestUri
     */
    public void storeLastActive(ID user, HttpServletRequest request, String requestUri) {
        if (requestUri == null) requestUri = request.getRequestURI();
        if (requestUri.contains("/filex/access/")
                || requestUri.contains("/notification/check-state")) {
            return;
        }

        HttpSession s = request.getSession();
        String loginChannel = (String) s.getAttribute(SK_LOGINCHANNEL);
        // 手机版
        if (loginChannel == null) {
            loginChannel = LoginChannel.parse(request.getHeader("user-agent"), true).name();
            s.setAttribute(SK_LOGINCHANNEL, loginChannel);
        }
        String key = user + ";" + loginChannel;
        ACTIVE_INFOS39.put(key, new ActiveInfo(user, request, requestUri));
    }

    /**
     * @param user
     * @param request
     * @param loginChannel
     */
    public void storeLoginSuccessed(ID user, HttpServletRequest request, LoginChannel loginChannel) {
        HttpSession s = request.getSession();
        s.setAttribute(SK_LOGINCHANNEL, loginChannel.name());
        final String key = user + ";" + loginChannel.name();

        // 不允许多用户登录
        if (!RebuildConfiguration.getBool(ConfigurationItem.MultipleSessions)) {
            HttpSession previous = getSession(user, loginChannel);
            if (previous != null) {
                log.warn("Kill previous session : {}, {}, {}", previous.getId(), user, loginChannel);

                try {
                    previous.invalidate();
                } catch (Throwable ignored) {}
            }
        }

        ONLINE_USERS.put(key, s);
    }

    /**
     * @param sessionOrTokenOrUser
     */
    public void killSession(Object sessionOrTokenOrUser) {
        // 用户 killall
        if (sessionOrTokenOrUser instanceof ID) {
            for (LoginChannel ch : LoginChannel.values()) {
                HttpSession s = getSession((ID) sessionOrTokenOrUser, ch);
                if (s != null) killSession(s.getId());
            }
            return;
        }

        // SessionId or AuthToken
        for (Map.Entry<String, ActiveInfo> e : getAllActiveInfos().entrySet()) {
            String key = e.getKey();
            ActiveInfo info = e.getValue();

            // H5
            if (sessionOrTokenOrUser.equals(info.getAuthToken())) {
                AuthTokenManager.verifyToken((String) sessionOrTokenOrUser, true, false);
                ACTIVE_INFOS39.remove(key);
                log.warn("Kill session : {}, {}", sessionOrTokenOrUser, key);
                break;
            }
            // PC
            if (sessionOrTokenOrUser.equals(info.getSessionId())) {
                for (HttpSession s : ONLINE_USERS.values()) {
                    if (sessionOrTokenOrUser.equals(s.getId())) {
                        s.invalidate();
                        ACTIVE_INFOS39.remove(key);
                        log.warn("Kill session : {}, {}", sessionOrTokenOrUser, key);
                        break;
                    }
                }
            }
        }
    }

    // --

    /**
     */
    @Getter
    public static class ActiveInfo {

        long activeTime;
        String activeUri;
        String activeIp;
        ID user;
        String sessionId;  // PC
        String authToken;  // H5

        protected ActiveInfo(ID user, HttpServletRequest request, String requestUri) {
            this.activeTime = System.currentTimeMillis();
            this.activeUri = StringUtils.defaultIfBlank(requestUri, request.getRequestURI());
            this.activeIp = ServletUtils.getRemoteAddr(request);
            this.user = user;
            this.sessionId = request.getSession().getId();
            this.authToken = request.getHeader(AppUtils.HF_AUTHTOKEN);
        }
    }
}
