/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

/**
 * @author devezhao
 * @since 2021/11/19
 * @see java.util.concurrent.Callable
 */
public interface Callable2<V, T> {

    /**
     * @param argv
     * @return
     */
    V call(T argv);
}
