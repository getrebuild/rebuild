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

    private static final ThreadLocal<ID> CALLER = new NamedThreadLocal<>("User in current thread");
    private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Request Locale");
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
     * @return
     */
    public static ID setUser(ID user) {
        Assert.notNull(user, "[user] cannot be null");

        ID o = CALLER.get();
        CALLER.set(user);
        return o;
    }

    /**
     * 设置语言/区域
     *
     * @param locale
     * @return
     */
    public static String setLocale(String locale) {
        Assert.notNull(locale, "[locale] cannot be null");

        String o = LOCALE.get();
        LOCALE.set(locale);
        return o;
    }

    /**
     * @return
     * @see #setUser(ID)
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
        clearUser(null);
    }

    /**
     */
    public static void clearLocale() {
        LOCALE.remove();
    }

    /**
     * @param restoreUser
     */
    public static void clearUser(ID restoreUser) {
        if (restoreUser == null) CALLER.remove();
        else CALLER.set(restoreUser);
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
