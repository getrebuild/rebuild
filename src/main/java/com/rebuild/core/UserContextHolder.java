/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

/**
 * 请求用户上下文（线程量）。
 * 请求后台方法时后台会从此类获取调用用户，如果是从 WEB 端请求，那么 MVC 请求拦截器会自动设置
 *
 * @author devezhao
 * @since 2020/9/27
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<ID> CALLER = new NamedThreadLocal<>("Current user");
    private static final ThreadLocal<ID> CALLER_PREV = new NamedThreadLocal<>("Previous user");

    private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Request locale");

    private static final ThreadLocal<String> REQIP = new NamedThreadLocal<>("Request IP");

    private UserContextHolder() {}

    /**
     * @param user
     * @param locale
     */
    public static void set(ID user, String locale) {
        setUser(user);
        setLocale(locale);
    }

    /**
     * 设置当前用户
     *
     * @param user
     * @see #replaceUser(ID)
     */
    public static void setUser(ID user) {
        Assert.notNull(user, "[user] cannot be null");

        ID e = getUser(Boolean.TRUE);
        if (e != null) {
            log.warn("Replace user in current thread (session) : {} < {}", user, e);
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
        return getUser(Boolean.FALSE);
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
        throw new AccessDeniedException("No user found in current session (thread)");
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
        REQIP.remove();
    }

    /**
     */
    public static void clearUser() {
        CALLER.remove();
        CALLER_PREV.remove();
    }

    /**
     */
    public static void clearLocale() {
        LOCALE.remove();
    }

    /**
     * 设置当前用户，并保持原始用户（如有）
     *
     * @param user
     * @see #getReplacedUser()
     * @see #restoreUser()
     */
    public static void replaceUser(ID user) {
        Assert.notNull(user, "[user] cannot be null");

        // Keep origin
        ID e = CALLER_PREV.get();
        if (e == null) e = getUser(Boolean.TRUE);

        if (e != null) CALLER_PREV.set(e);
        else CALLER_PREV.remove();

        CALLER.set(user);
    }

    /**
     * 获取原始用户
     * 
     * @return
     * @see #replaceUser(ID)
     */
    public static ID getReplacedUser() {
        ID prev = CALLER_PREV.get();
        if (prev != null) return prev;
        return getUser();
    }

    /**
     * @return
     * @see #replaceUser(ID)
     */
    public static boolean restoreUser() {
        ID e = CALLER_PREV.get();
        if (e != null) {
            clearUser();
            setUser(e);
            return true;
        }
        return false;
    }

    // --

    /**
     * @param reqip
     */
    public static void setReqip(String reqip) {
        Assert.notNull(reqip, "[reqip] cannot be null");
        REQIP.set(reqip);
    }

    /**
     * @return
     */
    public static String getReqip() {
        return REQIP.get();
    }
}
