/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.DefinedException;
import com.rebuild.core.support.License;

/**
 * @author devezhao
 * @since 2020/12/8
 */
public class RbAssert {

    /**
     * @param message
     */
    public static void isCommercial(String message) {
        if (!License.isCommercial()) {
            throw new DefinedException(message);
        }
    }

    /**
     * @param expression
     * @param message
     */
    public static void isAllow(boolean expression, String message) {
        if (!expression) {
            throw new DefinedException(message);
        }
    }
}
