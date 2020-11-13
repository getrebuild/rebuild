/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

/**
 * 请求用户上下文（线程量）。
 * 请求后台方法时后台会从此类获取调用用户，如果是从 WEB 端请求，那么 MVC 请求拦截器会自动设置
 *
 * @author devezhao
 * @since 2020/9/27
 */
public class UserContextHolder {

    private static final Logger LOG = LoggerFactory.getLogger(UserContextHolder.class);

    private static final ThreadLocal<ID> CALLER = new NamedThreadLocal<>("Current user");

    private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Current locale");

    private UserContextHolder() { }

    /**
     * @param user
     * @param locale
     */
    public static void set(ID user, String locale) {
        setUser(user);
        setLocale(locale);
    }

    /**
     * @param user
     */
    public static void setUser(ID user) {
        Assert.notNull(user, "[user] cannot be null");

        ID exists = getUser(true);
        if (exists != null) {
            LOG.warn("Replace user in current session (thread) : " + exists + " > " + user);
            CALLER.remove();
        }
        CALLER.set(user);
    }

    /**
     * @param locale
     */
    public static void setLocale(String locale) {
        Assert.notNull(locale, "[locale] cannot be null");

        LOCALE.set(locale);
    }

    /**
     * @return
     */
    public static ID getUser() {
        return getUser(false);
    }

    /**
     * @param allowNull
     * @return
     * @throws AccessDeniedException If user not in session
     */
    public static ID getUser(boolean allowNull) throws AccessDeniedException {
        ID user = CALLER.get();
        if (user != null) return user;

        if (allowNull) return null;
        else throw new AccessDeniedException("No user found in current session (thread)");
    }

    /**
     * @return
     */
    public static String getLocale() {
        String local = LOCALE.get();
        if (local != null) return local;

        // Use default
        return RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
    }

    /**
     */
    public static void clear() {
        clearUser();
        clearLocale();
    }

    /**
     */
    public static void clearUser() {
        CALLER.remove();
    }

    /**
     */
    public static void clearLocale() {
        LOCALE.remove();
    }
}
