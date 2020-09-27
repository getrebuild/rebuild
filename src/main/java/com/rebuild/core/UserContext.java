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
import org.springframework.core.NamedThreadLocal;

/**
 * 请求用户上下文
 *
 * @author devezhao
 * @since 2020/9/27
 */
public class UserContext {

    private static final ThreadLocal<ID> CALLER = new NamedThreadLocal<>("Current user");

    private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Current locale of user");

    public static void set(ID user, String locale) {
        setUser(user);
        setLocale(locale);
    }

    public static void clear() {
        CALLER.remove();
        LOCALE.remove();
    }

    public static void setUser(ID user) {
        CALLER.set(user);
    }

    public static void setLocale(String locale) {
        LOCALE.set(locale);
    }

    public static ID getUser() {
        return getUser(false);
    }

    public static ID getUser(boolean allowNull) {
        ID user = CALLER.get();
        if (user != null) return user;

        if (allowNull) return null;
        else throw new AccessDeniedException("No user found in current session");
    }

    public static String getLocale() {
        String local = LOCALE.get();
        if (local != null) return local;

        // Use default
        return RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
    }
}
