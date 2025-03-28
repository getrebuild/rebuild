/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.DefinedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.License;
import com.rebuild.core.support.NeedRbvException;
import com.rebuild.core.support.i18n.Language;

/**
 * @author devezhao
 * @since 2020/12/8
 */
public class RbAssert {

    /**
     * @param message
     */
    public static void isCommercial(String message) {
        if (License.isRbvAttached()) return;
        if (message == null) message = Language.L("免费版不支持此功能");
        throw new NeedRbvException(message);
    }

    /**
     * @param user
     */
    public static void isSuperAdmin(ID user) {
        is(UserHelper.isSuperAdmin(user), Language.L("非超级管理员用户"));
    }

    /**
     * @param expression
     * @param message
     * @see #is(boolean, String)
     */
    public static void isAllow(boolean expression, String message) {
        is(expression, message);
    }

    /**
     * @param expression
     * @param message
     */
    public static void is(boolean expression, String message) {
        if (!expression) {
            throw new DefinedException(message);
        }
    }

    /**
     * @param expression
     * @see #is(boolean, String)
     */
    public static void checkAllow(boolean expression) {
        is(expression, "Not Allow");
    }
}
