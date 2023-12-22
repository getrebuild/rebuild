/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

/**
 * Thread-Safe <tt>Observer</tt>
 *
 * @author devezhao
 * @since 2023/12/22
 */
public interface SafeObserver {

    void update(SafeObservable o, Object arg);

    int getOrder();
}
